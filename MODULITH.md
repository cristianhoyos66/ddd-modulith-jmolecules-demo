# Spring Modulith in this project

[Spring Modulith](https://spring.io/projects/spring-modulith) takes the idea of a **modular
monolith** — bounded contexts that are packaged together but isolated logically — and makes it
enforceable, testable, and documentable by the Spring ecosystem.

## Why it's here

A modular monolith buys you:

- **Deployment simplicity** of a monolith
- **Evolution path** to microservices (each module is a candidate to extract later)
- **Strong boundaries** enforced at build time, not wiki time

Without Modulith, "modules" are a convention. Someone adds a line like
`import com.example.demo.customers.domain.CustomerRepository` in the orders module and nothing
catches it — until six months later when customers can't be extracted because ten other modules
have reached into it.

With Modulith, that import would fail `ApplicationModules.verify()` as an illegal dependency.
The boundary is a check.

## How we define modules

Top-level packages under `com.example.demo` are treated as modules by Modulith:

```
com.example.demo/
├── DemoApplication.java        (bootstrap — not a module)
├── customers/                  ← module
├── catalog/                    ← module
├── inventory/                  ← module
└── orders/                     ← module
```

No `@ApplicationModule` annotation needed — convention over configuration. The `package-info.java`
at the root of each module (optionally) carries metadata (e.g. allowed dependencies, display
name).

### Within a module: public vs internal

Modulith's default: **root package = public API; sub-packages = internal.** A class in
`customers.application.CustomerService` is internal to customers — other modules cannot import it
unless we publish it.

This project uses four sub-packages per module:

- `domain/` — aggregates, VOs, events, repository interfaces
- `application/` — use cases (services + commands + views)
- `infrastructure/` — listeners, external adapters
- `web/` — controllers, DTOs

We explicitly publish the packages that other modules need to see.

### Named interfaces — publishing sub-packages

```java
// customers/domain/package-info.java
@org.springframework.modulith.NamedInterface
@org.jspecify.annotations.NullMarked
package com.example.demo.customers.domain;
```

```java
// customers/application/package-info.java
@org.springframework.modulith.NamedInterface
@org.jspecify.annotations.NullMarked
package com.example.demo.customers.application;
```

Both sub-packages are `@NamedInterface` so the `orders` module can import `Customer.CustomerIdentifier`
(from `customers.domain`) and `CustomerService` (from `customers.application`).

Packages we don't annotate (like `customers.infrastructure`, `customers.web`) stay private.
Another module that imports `customers.web.CustomersController` would fail verification.

## Cross-module communication

We use two paths, deliberately distinguished:

### Writes → domain events

When something happens in module A that module B should react to, A publishes a domain event.
B registers a listener. A never knows who listens; B never calls into A.

```java
// catalog/domain/Product.java (aggregate)
public Product(String name, Money price) {
  ...
  registerEvent(new ProductAdded(id, name));   // event attached, published after save
}
```

```java
// inventory/infrastructure/InventoryListener.java
@Component
class InventoryListener {
  @ApplicationModuleListener
  void on(ProductAdded event) {
    inventory.track(event.productId());
  }
}
```

`@ApplicationModuleListener` is the important annotation. It's a composite that expands to:

| Underlying annotation | Effect |
|---|---|
| `@TransactionalEventListener(phase = AFTER_COMMIT)` | Listener runs only after the publishing transaction commits — rolled-back events never fire |
| `@Async` | Runs on a different thread, so a slow listener doesn't block the publisher |
| `@Transactional(propagation = REQUIRES_NEW)` | Listener body gets its own transaction |

Combined: each listener is its own atomic unit, eventually consistent with the publisher.

**Persistence:** events are stored in the `event_publication` table (managed by
`spring-modulith-starter-jpa`) so a crash before the listener runs doesn't lose the event. The
registry can replay incomplete publications on restart.

### Reads → published application-service use cases

For synchronous queries ("does this customer exist?", "what's the price of this product?"),
we publish a narrow read API on the target module's application service and the caller invokes it
directly:

```java
// orders/application/OrderService.java
public OrderView place(PlaceOrderCommand command) {
  var customerId = new Customer.CustomerIdentifier(command.customerId());
  if (!customers.exists(customerId)) {          // ← cross-module read via CustomerService
    throw new UnknownCustomer(customerId);
  }
  ...
}
```

We discussed and rejected the alternative — each module maintaining its own local projection of
customer data via `CustomerRegistered` events. That pattern is easy to copy-paste into every
module and ends up with N near-identical projections. One published use case is cleaner for
reads in a single-process monolith.

**When the module becomes a separate service,** the same interface survives — `CustomerService`
becomes a remote call (HTTP/gRPC), and only its implementation moves. The caller doesn't change.

## Verification — `ApplicationModules.verify()`

```java
// ModulithStructureTests.java
@Test
void verifiesModularStructure() {
  ApplicationModules.of(DemoApplication.class).verify();
}
```

This runs on every `./mvnw test`. It fails the build if:

- A module imports a type from another module's non-public package
- Cyclic dependencies between modules are introduced
- A class in a `@NamedInterface` package references an internal type

Try breaking it — add this line to `OrderService`:
```java
import com.example.demo.customers.infrastructure.SomeInternal;
```
and verification fails with a specific, actionable message: "orders depends on internal type
`customers.infrastructure.SomeInternal`".

## What would break module semantics

### Violation 1 — reaching into another module's internal package

```java
// orders/application/OrderService.java
import com.example.demo.customers.web.CustomersController;   // ← customers.web is internal
```

**Failure:** `Module 'orders' depends on non-exposed type in module 'customers'`.

**Fix:** don't call the controller from another module. Use the published application service.

### Violation 2 — circular dependency

If `orders` depends on `customers` and you add a dependency from `customers` → `orders`, verification fails.

**Failure:** `Cyclic dependency detected: customers -> orders -> customers`.

**Fix:** break the cycle via an event. If customers needs to know something happens in orders,
listen for `OrderPlaced` / `OrderCompleted` instead of calling into orders directly.

### Violation 3 — synchronous listener instead of `@ApplicationModuleListener`

```java
@EventListener
void on(OrderPlaced event) { ... }   // ← synchronous, same thread, same tx
```

This isn't flagged by `verify()`, but it's a modulith anti-pattern. A listener that runs in the
publisher's transaction couples the two modules' transactional fates: if the listener fails, the
publisher rolls back. Use `@ApplicationModuleListener`.

### Violation 4 — forgetting `@NamedInterface`

Adding a public type in `customers.application` without `@NamedInterface` on
`package-info.java` means orders can't import it, and you'll get:

**Failure:** `orders.application.OrderService depends on non-exposed type customers.application.CustomerService`.

**Fix:** annotate `package-info.java`.

## Module docs generation

`ModulithStructureTests.writesDocumentation()` calls:

```java
new Documenter(modules).writeDocumentation();
```

After running tests, `target/spring-modulith-docs/` contains:

- `components.puml` — C4 component diagram of the whole app, with module dependencies drawn
- `module-<name>.puml` — a C4 diagram per module
- `module-<name>.adoc` — AsciiDoc tables listing aggregates, published events, services, bean references
- `all-docs.adoc` — index that pulls them together

Open any `.puml` in a PlantUML renderer to visualise the module graph. These are useful for
architecture reviews or onboarding.

## `@ApplicationModuleTest`

For module-level isolation tests:

```java
@ApplicationModuleTest
class CustomersModuleTests {
  @Autowired CustomerService service;
  ...
}
```

Boots a Spring context with just the tested module and its declared dependencies. Other modules'
beans are absent. This is how we prove each module is individually runnable.

`PublishedEvents` (injected as a test parameter) lets you assert on events that fired during the
test:

```java
@Test
void registeringACustomerPublishesCustomerRegistered(PublishedEvents events) {
  var view = service.register(new RegisterCustomerCommand("Alice", "alice@example.com"));

  assertThat(events
      .ofType(CustomerRegistered.class)
      .matching(e -> e.customerId().id().equals(view.id())))
    .hasSize(1);
}
```

## The runtime actuator (not enabled)

If you add `spring-modulith-actuator`, `GET /actuator/modulith` returns the live module graph as
JSON. Handy for debugging or a live architecture dashboard. Not included here to keep the
dependency surface small.

## How to see it work

```bash
# Verify module structure
./mvnw test -Dtest=ModulithStructureTests

# Inspect generated diagrams/docs
open target/spring-modulith-docs/components.puml    # or any .puml / .adoc

# Prove a module boots in isolation
./mvnw test -Dtest=CustomersModuleTests

# Full event-driven flow, end to end
./mvnw test -Dtest=EndToEndOrderFlowTests
```

Try adding an illegal cross-module import (e.g. `orders` importing from `customers.infrastructure`)
and rerun `ModulithStructureTests`. It will fail before any other tests run.
