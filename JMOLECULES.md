# jMolecules in this project

[jMolecules](https://github.com/xmolecules/jmolecules) turns DDD and other architectural concepts
into code you can reference, verify, and generate from. It's the vocabulary we use throughout
the codebase to say what each class *is* in DDD terms.

## Why it's here

Without jMolecules, a class called `Customer` is just a class. You'd put `@Entity` on it, `@Id` on
its id field, write a default constructor for Hibernate, decide the equals/hashCode contract, and
remember it's supposed to be an aggregate root via comments or convention.

With jMolecules:

1. **We declare intent in types** — `Customer implements AggregateRoot<Customer, CustomerIdentifier>`. No Javadoc archaeology to find out what a class is.
2. **Tools generate the framework plumbing** — the ByteBuddy plugin sees `AggregateRoot` and adds `@Entity`, identity-based `equals`/`hashCode`, lifecycle hooks, and `Persistable` optimizations at compile time. Our source files stay free of persistence annotations.
3. **Tools enforce the DDD rules** — `JMoleculesDddRules.all()` runs as an ArchUnit test and fails the build if a VO is mutable, an aggregate holds a direct reference to another aggregate, etc.

The demo uses all three: **vocabulary**, **integration** (JPA via ByteBuddy, Jackson serialization), and **verification** (ArchUnit rules).

## The vocabulary — what we use and where

| Concept | Type / Annotation | Where it's used |
|---|---|---|
| Aggregate root | `AggregateRoot<T, ID>` (interface) | `Customer`, `Product`, `InventoryItem`, `Order` |
| Entity (inside an aggregate) | `Entity<AggregateT, ID>` (interface) | `LineItem` (inside `Order`) |
| Value object | `@ValueObject` annotation | `EmailAddress`, `Money` |
| Identifier | `Identifier` (marker interface) | `CustomerIdentifier`, `ProductIdentifier`, `OrderIdentifier`, … |
| Association (to another aggregate) | `Association<T, ID>` | `LineItem.product`, `InventoryItem.product`, `Order.customer` |
| Domain event | `@DomainEvent` annotation | `CustomerRegistered`, `ProductAdded`, `OrderPlaced`, `OrderCompleted`, `OutOfStock` |
| Domain/Application service | `@org.jmolecules.ddd.annotation.Service` | `CustomerService`, `CatalogService`, `InventoryService`, `OrderService`, `OrderPricing` |

**Identifiers** are `record`s that `implements Identifier`. A single UUID field is the norm.

**Associations** are how we reference another aggregate by id. `Order` does not hold a `Customer`
field — it holds `Association<Customer, Customer.CustomerIdentifier>`. This is an invariant
enforced by `JMoleculesDddRules` (see below).

## The integrations we rely on

### ByteBuddy plugin (compile-time JPA mapping)

Configured in `pom.xml` under `byte-buddy-maven-plugin` with the jMolecules plugin. After
compilation it scans class files, finds DDD interfaces (`AggregateRoot`, `Entity`, `Identifier`,
`ValueObject`), and rewrites the bytecode to add:

- JPA annotations (`@Entity`, `@Id`, `@Embeddable`, `@AttributeOverrides`)
- A no-arg constructor required by Hibernate
- `equals()` / `hashCode()` based on identity for aggregates
- `__jMolecules__PrePersist` / `__jMolecules__PostLoad` lifecycle hooks
- `Persistable<ID>.isNew()` for Spring Data save-vs-merge optimization

Run `javap -p target/classes/com/example/demo/customers/domain/Customer.class` and you'll see
fields and methods that aren't in `Customer.java`. That's the plugin at work.

### Jackson integration

`jmolecules-jackson3` teaches Jackson to serialize value objects transparently. A
`CustomerIdentifier(UUID id)` serializes as the bare UUID string, not `{"id":"..."}`. This keeps
our HTTP API JSON shape clean without us writing custom serializers.

### Spring integration

`jmolecules-spring` treats the jMolecules `@Service` annotation as a Spring stereotype, so classes
marked with `@org.jmolecules.ddd.annotation.Service` are picked up as beans without a second
`@Component`.

## The rules — what would break DDD and fail the build

`JMoleculesRulesTests.enforcesDddRules()` runs `JMoleculesDddRules.all()`. Concrete examples of
what would fail:

### Rule 1 — aggregate references another aggregate directly

**Violation:**
```java
public class Order implements AggregateRoot<Order, OrderIdentifier> {
  private Customer customer;   // ← holding a Customer object
}
```

**Failure:** `Aggregate Order depends on aggregate Customer outside of an Association`.

**Fix:**
```java
private Association<Customer, Customer.CustomerIdentifier> customer;
```

### Rule 2 — value object is mutable

**Violation:**
```java
@ValueObject
public class EmailAddress {
  private String address;
  public void setAddress(String a) { this.address = a; }  // ← mutator
}
```

**Failure:** `Value object EmailAddress is not immutable`.

**Fix:** make it a record or a class with all-final fields and no mutators.

### Rule 3 — entity without identity

**Violation:**
```java
public class LineItem implements Entity<Order, LineItemIdentifier> {
  // no getId(), no @Identity field
}
```

**Failure:** `Entity LineItem has no identity`.

**Fix:** implement `getId()` (the interface forces it) or annotate a field with `@Identity`.

### Rule 4 — identifier type doesn't implement `Identifier`

**Violation:**
```java
public record OrderIdentifier(UUID id) {}   // ← doesn't implement Identifier
```

**Failure:** `OrderIdentifier used as id type but does not implement Identifier`.

**Fix:**
```java
public record OrderIdentifier(UUID id) implements Identifier {}
```

### Rule 5 — repository lives outside the aggregate's module

**Violation:** putting `OrderRepository` inside `customers.domain` while `Order` lives in `orders.domain`.

**Failure:** the rule set detects the package mismatch and fails.

**Fix:** keep repositories next to the aggregate they manage.

### Rule 6 — one aggregate's code reaches into another aggregate's internals

Related to rule 1 but broader. If `InventoryItem` were to call methods on a loaded `Product`
aggregate directly (not via Association), the rule would flag it.

## Why we keep it narrow

`JMoleculesDddRules.all()` is a bundle. You can also compose individual rules if you want a
different subset — e.g., only aggregates-via-association and not the VO immutability check.
We use the whole bundle because the demo wants the full picture.

Rules *not* enforced here (but possible):

- **CQRS** rules (`@ReadOnly`, `@WriteOnly`) — we don't use CQRS-style separation
- **Layered architecture** rules — Modulith handles our layering concerns via package boundaries

## How to see it work

```bash
# Run just the DDD rules test
./mvnw test -Dtest=JMoleculesRulesTests

# Inspect a transformed class to see ByteBuddy's additions
javap -p target/classes/com/example/demo/customers/domain/Customer.class
```

The test passes today. Try adding `private Customer customer` to `Order.java`, rerun, and watch
the build break with a specific, readable violation message.
