---
paths:
  - "src/main/java/com/example/demo/*/domain/**/*.java"
---

# Rules for the `domain/` layer

Loaded only when working on files under any module's `domain/` package. Authoritative reference
in [DDD.md](../../DDD.md) and [JMOLECULES.md](../../JMOLECULES.md); these are the rules the
`JMoleculesRulesTests` and `ModulithStructureTests` will enforce.

## Aggregates

- Class (not record), extends `org.springframework.data.domain.AbstractAggregateRoot<T>`, implements `org.jmolecules.ddd.types.AggregateRoot<T, Id>`.
- Reference other aggregates **only** via `Association<Aggregate, Identifier>` — never a direct object reference.
- Raise domain events in the constructor / mutating methods using `registerEvent(new FooEvent(...))`. Never raise events from the application service.
- Identifier is a nested `record FooIdentifier(UUID id) implements Identifier`.

## Value objects

- `record` with final fields and validation in the compact constructor.
- `@org.jmolecules.ddd.annotation.ValueObject` is optional but preferred as a marker.

## Domain events

- `record` with `@org.jmolecules.event.annotation.DomainEvent`.
- Carry identifiers (`*Identifier`) and immutable VOs. No aggregate references.

## Repositories

- Interface in the same `domain/` package as the aggregate it serves.
- Extends `org.springframework.data.repository.CrudRepository<Aggregate, Identifier>`.
- No implementation file — Spring Data generates at runtime.

## Domain services

- Stateless. No I/O. No repositories. No `@Transactional`.
- Annotated `@org.jmolecules.ddd.annotation.Service`.
- Takes aggregates (+ any pre-fetched data the caller supplies) as parameters.
- Example: `OrderPricing.totalFor(Order order, Map<ProductIdentifier, Money> prices)`.

## Package-info

- `@org.springframework.modulith.NamedInterface` on `package-info.java` — domain types are the module's public API, other modules import IDs/events/VOs from here.
- `@org.jspecify.annotations.NullMarked` on every `package-info.java`.

## Don't

- Don't add `@Entity`, `@Id`, `@GeneratedValue`, `@Embeddable`, `@Table`, or any JPA annotations — the jMolecules ByteBuddy plugin generates them from the tactical interfaces.
- Don't reach into other modules' `domain/` for anything other than IDs, VOs, and events.
- Don't inject framework dependencies (`ApplicationEventPublisher`, repositories, config properties) into aggregates.
- Don't add `@Transactional` anywhere in `domain/`.
