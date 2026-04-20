---
paths:
  - "src/main/java/com/example/demo/*/application/**/*.java"
---

# Rules for the `application/` layer

Loaded only when working on files under any module's `application/` package. This is the
module's use-case surface.

## Application services

- Class annotated `@org.jmolecules.ddd.annotation.Service`.
- Method per use case. Name the method after the use case (`register`, `place`, `calculateTotal`).
- Inputs: a `Command` record (when the method takes user intent) or a primitive/ID (for simple lookups).
- Outputs: a `View` record, **never** a domain aggregate.
- Orchestrates: loads aggregates, calls domain methods and domain services, saves, builds the View.

## Commands

- Plain `record` with the exact fields the use case needs.
- Nested records for sub-structures (`PlaceOrderCommand.Line`).
- No behavior — commands are just parameter bundles.

## Views

- `record` with primitive / id types (not domain aggregates).
- Package-private `static from(Aggregate)` factory.
- Shape matches what callers need; flatten domain types to plain values.

## Ports (for external systems)

- Interface in `application/`, named after the capability the use case needs (`WelcomeNotifier`, not `SendGridClient`).
- Method signatures use **domain types**, not HTTP/infra types.
- Implementation lives in `infrastructure/` (see infrastructure rules).

## `@Transactional`

Method-level, only when one of:

- The method performs multiple repository operations that must be atomic (`findById` + mutate + `save`).
- The method traverses lazy-loaded associations during view mapping (read-only: add `@Transactional(readOnly = true)`).

Do NOT add `@Transactional` to:

- Single-statement methods. Spring Data wraps them internally.
- Class level (class-level `@Transactional` is avoided in this codebase for explicitness).

## Cross-module calls

- Reads → call another module's application service directly (its package must be `@NamedInterface`).
- Writes → publish a domain event from the aggregate; let a listener in the consumer module react.
- **Never** maintain a local projection of another module's data (see the `CustomerLookup` rejection in `DDD.md`).

## Package-info

- `@org.springframework.modulith.NamedInterface` on `package-info.java` if the application service is called from another module (our `customers` and `catalog` modules do this — see their `application/package-info.java`).
- `@org.jspecify.annotations.NullMarked`.

## Don't

- Don't return domain aggregates from public methods.
- Don't inject a `ApplicationEventPublisher` — events are raised by aggregates, not services.
- Don't put business rules here that could live on the aggregate or in a domain service.
- Don't import controllers, DTOs, or web types.
