# DDD approaches in this project

This document collects the **tactical DDD decisions** we made and why. It's a recipe book for
future contributors: "when X comes up, here's how we handle it and what the alternative would
have cost."

If you're new to the codebase, read [README.md](README.md) first for the bird's-eye view, then
this for *how* and *why* things are the way they are.

## Package structure: feature-first, then layered

Top level is **bounded context**. Inside each context, four layers:

```
<module>/
├── domain/           (aggregates, VOs, events, repository interfaces)
├── application/      (services, commands, views)
├── infrastructure/   (listeners, external adapters)
└── web/              (controllers, request/response DTOs)
```

**Why not layer-first (`application/customers`, `domain/customers`, …)?**
Spring Modulith defines modules by top-level package. A layer-first layout would collapse every
bounded context into one giant "application" module and destroy the boundary check we're paying
Modulith to enforce. Cohesion also wins in practice: a typical feature change touches one module
end-to-end, not four top-level layer folders.

## The dependency rule

Arrows of allowed import direction:

```
web → application → domain
                       ▲
infrastructure ────────┘
```

- `domain/` imports nothing from its siblings.
- `application/` imports `domain/` only.
- `infrastructure/` imports `domain/` and `application/` (it implements the ports and reacts to events).
- `web/` imports `application/` (and `domain/` types that flow through, like ids).

Violating this shows up as an ugly import and — for cross-module imports — a Modulith `verify()` failure.

## Aggregates

- A class, not a record (state has to be mutable for JPA to hydrate it).
- Extends `AbstractAggregateRoot<T>` (Spring Data) and implements `AggregateRoot<T, Id>` (jMolecules).
- Constructor enforces invariants and **registers domain events** describing what just happened.
- Identity via a nested `record *Identifier(UUID id) implements Identifier {}`.

Example — `customers/domain/Customer.java`:

```java
public class Customer extends AbstractAggregateRoot<Customer>
    implements AggregateRoot<Customer, Customer.CustomerIdentifier> {

  public Customer(String name, EmailAddress email) {
    this.id = new CustomerIdentifier(UUID.randomUUID());
    this.name = name;
    this.email = email;
    registerEvent(new CustomerRegistered(id, email));
  }

  public record CustomerIdentifier(UUID id) implements Identifier {}
}
```

### Where we raise events — in the aggregate

The fact "a customer was registered" is a **business fact**. Only the aggregate knows all the
invariants are satisfied and the fact is true. Registering it on the aggregate means any code
path that creates a customer emits the event automatically.

`registerEvent()` attaches it to the aggregate's event list. **Publication happens later**, in
the same transaction, after `repository.save()`. That split keeps the domain unaware of the
publishing mechanism.

### Cross-aggregate references — `Association<T, Id>`

Aggregates must **never hold a direct reference** to another aggregate. Use
`Association<Aggregate, Identifier>` instead. See `orders/domain/Order.java`:

```java
private Association<Customer, Customer.CustomerIdentifier> customer;
private List<LineItem> lineItems = new ArrayList<>();   // entities within this aggregate
```

`LineItem` is inside the `Order` aggregate (an `Entity<Order, LineItemIdentifier>`). `Customer` is
in a different aggregate, so we only hold its id via `Association`.

`JMoleculesDddRules` enforces this at build time. See [JMOLECULES.md](JMOLECULES.md) for details.

## Value objects

- Records implementing nothing (or `@ValueObject` annotated if you want the marker).
- All fields final, validation in the compact constructor.

```java
@ValueObject
public record EmailAddress(String address) {
  public EmailAddress {
    if (address == null || !address.contains("@")) {
      throw new IllegalArgumentException("Invalid email address: " + address);
    }
  }
}
```

## Repositories

Interface in `domain/`, extending Spring Data's `CrudRepository`. Spring generates the
implementation at runtime — no separate impl class.

```java
public interface CustomerRepository
    extends CrudRepository<Customer, Customer.CustomerIdentifier> {}
```

**Why coupled to Spring Data in domain?** Pragmatic call. The alternative — plain interface in
domain + Spring Data bridge in infrastructure — is textbook pure but adds two classes and a
mapping layer for every aggregate. The Spring Data interface is stable enough that the coupling
isn't a real portability risk.

If you ever need the strict split (e.g. moving to a non-Spring stack), the interface still
describes the contract and you can write a pure adapter against it.

## Application services: the module's use-case surface

A class per module, in `application/`. Methods are named use cases. They:

1. Accept a **Command** record as input.
2. Orchestrate: load aggregates, call domain methods, save.
3. Return a **View** record (never a domain aggregate — see below).

```java
@Service
public class OrderService {

  @Transactional
  public OrderView place(PlaceOrderCommand command) {
    var customerId = new Customer.CustomerIdentifier(command.customerId());
    if (!customers.exists(customerId)) throw new UnknownCustomer(customerId);
    var order = new Order(customerId);
    command.lines().forEach(line ->
        order.addLine(new Product.ProductIdentifier(line.productId()), line.quantity()));
    return OrderView.from(orders.save(order.place()));
  }
}
```

### Commands — plain records, not a bus

```java
public record PlaceOrderCommand(UUID customerId, List<Line> lines) {
  public record Line(UUID productId, long quantity) {}
}
```

No mediator, no dispatcher. The service method *is* the command handler. Named parameter record
= explicit contract for what the use case needs.

### Views — what the application returns

The application service **never returns a domain aggregate**. It returns a `*View` record defined
in `application/`:

```java
public record CustomerView(UUID id, String name, String email) {
  static CustomerView from(Customer customer) {
    return new CustomerView(
        customer.getId().id(), customer.getName(), customer.getEmail().address());
  }
}
```

**Why:**

- The domain aggregate has behavior (`order.complete()`) that callers could accidentally invoke outside a use case.
- JPA lazy-loading would leak through — accessing `order.getLineItems()` after the session closes throws.
- Renaming a domain field would ripple into every controller and test.

The View is read-only, shape-only, owned by the application layer.

### Web DTOs — separate from views

Controllers use a **different** layer of records — `Request` and `Dto` in `web/`:

```java
record RegisterCustomerRequest(String name, String email) {}
record CustomerDto(UUID id, String name, String email) {
  static CustomerDto from(CustomerView view) { ... }
}
```

Today `CustomerDto` and `CustomerView` look identical. They exist separately so that when the
HTTP contract needs to diverge (versioning, field rename, hiding a field in v2) you touch `web/`
without changing the application layer.

## Cross-module communication

Two paths. Choose based on whether it's a read or a write.

### Writes → domain events

When something happens in module A that module B should know about, A publishes an event,
B listens. See `catalog → inventory` (`ProductAdded`) and `orders → inventory` (`OrderPlaced`).

```java
// inventory/infrastructure/InventoryListener.java
@Component
class InventoryListener {
  @ApplicationModuleListener
  void on(ProductAdded event) { inventory.track(event.productId()); }

  @ApplicationModuleListener
  void on(OrderPlaced event) {
    event.lines().forEach(line -> inventory.withdraw(line.productId(), line.quantity()));
  }
}
```

The publisher doesn't import the listener, doesn't know it exists. If inventory gets extracted
into its own service tomorrow, swap the in-process event bus for a message broker — publisher
code doesn't change.

### Reads → published application service

When module A needs to *ask a question* of module B, A calls B's application service through a
narrow published interface.

```java
// customers/CustomerService.java  — in application/, @NamedInterface'd
public boolean exists(Customer.CustomerIdentifier id) {
  return repository.existsById(id);
}
```

```java
// orders/application/OrderService.java
if (!customers.exists(customerId)) throw new UnknownCustomer(customerId);
```

### Why not event-driven projections for reads too?

We considered and rejected it. The pattern goes: orders subscribes to `CustomerRegistered`,
maintains a local `customer_lookup` table, queries it at place-order time.

Problems:
1. Every module that needs customer data writes the same listener + projection.
2. Keeping projections in sync with customer deletions/updates requires more events and more code.
3. Eventual consistency — a customer registered 5 ms ago doesn't exist to orders yet — causes tests to flake and UX to surprise users.

A single published read method is cleaner when you're in one process. If the module gets
extracted, that method becomes a remote call behind the same interface. The published-API path
generalizes; the projection path fragments.

## Domain service vs application service

The application service does **orchestration**. A domain service does **pure business logic
spanning multiple aggregates**.

In this project:

- `OrderPricing` (`orders/domain/OrderPricing.java`) is a **domain service**.
  - Rules: sum line totals, apply 10% discount above $100.
  - Takes the `Order` and a pre-fetched `Map<ProductId, Money>` of prices.
  - **No I/O.** No repositories. Testable without Spring.

- `OrderService.calculateTotal(id)` is the **application service** method.
  - Loads the order.
  - Calls `CatalogService.priceOf(...)` for each distinct product id.
  - Hands order + prices to `OrderPricing.totalFor(...)`.
  - Returns an `OrderTotalView`.

### Rule of thumb

| Where does the logic belong? | |
|---|---|
| Pure business rule within one aggregate | Method on the aggregate |
| Pure business rule spanning aggregates / stateless | **Domain service** (`domain/`) |
| Needs I/O, transactions, or cross-module calls | **Application service** (`application/`) |

## External calls — the port/adapter pattern

When the application needs something from outside (HTTP API, email service, payment gateway,
message queue), put the **interface in `application/`** and the **implementation in `infrastructure/`**.

Example — `customers/application/WelcomeNotifier.java`:

```java
public interface WelcomeNotifier {
  void welcome(Customer customer);
}
```

The interface is phrased in the use case's language (`welcome(customer)`), not the delivery
mechanism's (`post(url, json)`).

Implementation — `customers/infrastructure/LoggingWelcomeNotifier.java`:

```java
@Component
class LoggingWelcomeNotifier implements WelcomeNotifier {
  @Override
  public void welcome(Customer customer) {
    LOG.info("Welcome email dispatched to {} <{}>", ...);
  }
}
```

Swapping for an HTTP adapter (`HttpWelcomeNotifier` using `RestClient`) is a one-file change;
`CustomerService` never sees it.

### Why the interface is in `application/`, not `domain/`

Two defensible choices exist:

- **Application** (what we do): the need is a use-case concern — "when registering, notify." The domain doesn't care *whether* notification happens.
- **Domain**: Vernon sometimes puts service-style interfaces in the domain when the capability is a domain concept (e.g., `PasswordHasher`).

For a notification/email, the line is blurry. We default to application because the domain
doesn't reason about welcome emails. For anything with domain invariants attached
(`PaymentGateway.charge` returning success/failure that affects aggregate state), domain would be
the better fit.

## Transactions

Explicit `@Transactional` only where it earns its keep:

| Situation | `@Transactional`? |
|---|---|
| Single repository call | No (Spring Data wraps it internally) |
| Multi-step load-mutate-save | Yes |
| Query with lazy-loaded associations traversed during view mapping | `@Transactional(readOnly = true)` |
| Listener method | No (`@ApplicationModuleListener` already is) |
| Controller method | No (belongs in service) |

Class-level `@Transactional` is avoided in favor of explicit per-method annotations, so each
method's transactional intent is visible at the declaration site.

## Summary: where each concern lives

| Concern | Location | Example |
|---|---|---|
| Aggregate + invariants | `<module>/domain/` | `Order` |
| Value object | `<module>/domain/` | `EmailAddress`, `Money` |
| Domain event | `<module>/domain/` | `OrderPlaced` |
| Repository interface | `<module>/domain/` | `OrderRepository` |
| Domain service | `<module>/domain/` | `OrderPricing` |
| Custom exception | `<module>/domain/` | `UnknownCustomer` |
| Application service (use cases) | `<module>/application/` | `OrderService` |
| Command | `<module>/application/` | `PlaceOrderCommand` |
| View (read model) | `<module>/application/` | `OrderView` |
| External service port (interface) | `<module>/application/` | `WelcomeNotifier` |
| External service adapter (impl) | `<module>/infrastructure/` | `LoggingWelcomeNotifier` |
| Domain event listener | `<module>/infrastructure/` | `InventoryListener` |
| HTTP controller | `<module>/web/` | `OrdersController` |
| Request / Response DTO | `<module>/web/` | `PlaceOrderRequest`, `OrderDto` |

## Further reading

- [README.md](README.md) — project overview and how to run
- [JMOLECULES.md](JMOLECULES.md) — the DDD vocabulary and rules we enforce
- [MODULITH.md](MODULITH.md) — how module boundaries are defined and enforced
