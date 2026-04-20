# demo — tactical DDD with Spring Modulith and jMolecules

A reference Spring Boot 4 application that practices **tactical domain-driven design** inside a
**modular monolith**, using [jMolecules](JMOLECULES.md) for the DDD vocabulary and
[Spring Modulith](MODULITH.md) for module boundary enforcement.

Four bounded contexts — **customers**, **catalog**, **inventory**, **orders** — talk to each
other through domain events (writes) and a narrow published API (reads). No module reaches into
another's internals.

## Architecture at a glance

```
┌──────────────────────────┐     CustomerRegistered
│  customers               │──────────────────────────┐
│  Customer, EmailAddress  │                          │
│  CustomerService         │                          ▼
└──────────────────────────┘     ┌──────────────────────────────────┐
            ▲                    │  orders                          │
   CustomerService.exists()      │  Order, LineItem, OrderPricing   │
            │                    │  OrderService                    │
            │                    └──────────────────────────────────┘
            │                             │              ▲
            │                  OrderPlaced│              │ CatalogService
            │                             ▼              │    .priceOf()
┌──────────────────────────┐     ┌──────────────────────────────────┐
│  catalog                 │────▶│  inventory                       │
│  Product, Money          │     │  InventoryItem, InventoryService │
│  CatalogService          │ ▲   │                                  │
└──────────────────────────┘ │   └──────────────────────────────────┘
                             └── ProductAdded
```

- **Writes** cross module boundaries via events (`CustomerRegistered`, `ProductAdded`,
  `OrderPlaced`, `OrderCompleted`, `OutOfStock`). Publisher doesn't know who listens.
- **Reads** cross via the consumed module's application-service use cases (e.g.
  `CustomerService.exists(id)`, `CatalogService.priceOf(id)`) — a single narrow path, no duplicated
  projections.
- **External integrations** use the port/adapter pattern: interface in `application/`,
  implementation in `infrastructure/`.

## Module layout

Each module follows the same four-layer split:

```
<module>/
├── domain/           ← aggregates, value objects, events, repository interfaces
│                       (marked @NamedInterface — cross-module types live here)
├── application/      ← application services, commands, views
│                       (marked @NamedInterface where use cases are called cross-module)
├── infrastructure/   ← listeners, external adapters (HTTP clients, etc.) — internal
└── web/              ← controllers, request/response DTOs — internal
```

**Why feature-first (module-by-module) rather than layer-first:** Spring Modulith treats top-level
packages as modules; putting layers on top would collapse every bounded context into one module
and defeat the tool. Cohesion also wins in practice — changes to customers touch one folder, not
four.

## Running the app

### Prerequisites

- Java 25 (the project targets 25; `java -version` should print 25.x)
- Docker (the app uses Docker Compose to spin up Postgres automatically)

**macOS — installing Java 25**

```bash
brew install openjdk@25
```

Then either (a) set `JAVA_HOME` in your shell rc:

```bash
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home' >> ~/.zshrc
source ~/.zshrc
```

…or (b) register the JVM with macOS so `/usr/libexec/java_home` finds it:

```bash
sudo ln -sfn /opt/homebrew/opt/openjdk/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-25.jdk
```

Or just prefix each Maven command:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home ./mvnw verify
```

Claude Code sessions in this repo get JDK 25 automatically — `JAVA_HOME` is set in
`.claude/settings.local.json`.

### Start

```bash
./mvnw spring-boot:run
```

Spring Boot's Docker Compose integration (`spring-boot-docker-compose`) launches Postgres from
`compose.yaml` on first run. Flyway applies `src/main/resources/db/migration/V1__init.sql`.
Virtual threads are enabled (`spring.threads.virtual.enabled=true`).

### Exercise the flow via curl

```bash
# Register a customer (triggers WelcomeNotifier + publishes CustomerRegistered)
curl -X POST http://localhost:8080/customers \
  -H 'Content-Type: application/json' \
  -d '{"name":"Alice","email":"alice@example.com"}'

# Add a product (publishes ProductAdded → inventory creates zero-stock InventoryItem)
curl -X POST http://localhost:8080/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"Widget","price":"9.99","currency":"USD"}'

# Restock
curl -X POST http://localhost:8080/inventory/{productId}/restock \
  -H 'Content-Type: application/json' -d '{"amount":10}'

# Place an order (validates customer via CustomerService.exists;
#                publishes OrderPlaced → inventory decrements stock)
curl -X POST http://localhost:8080/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"<uuid>","lines":[{"productId":"<uuid>","quantity":3}]}'

# Calculate the order total (orchestrates CatalogService.priceOf + OrderPricing domain service)
curl http://localhost:8080/orders/{orderId}/total
```

## Testing

```bash
./mvnw test
```

Tests use H2 in-memory but with the **same Flyway migrations** that run against Postgres in prod,
so the schema is identical.

What the test suite enforces:

| Test | Purpose |
|---|---|
| `ModulithStructureTests` | Runs `ApplicationModules.verify()` — fails if a module imports another's internals |
| `JMoleculesRulesTests` | Runs `JMoleculesDddRules.all()` — fails if an aggregate references another directly, if a VO is mutable, etc. |
| `CustomersModuleTests`, `CatalogModuleTests` | `@ApplicationModuleTest` slicing — proves each module boots in isolation |
| `EndToEndOrderFlowTests` | Full `@SpringBootTest` scenario verifying the event chain across all four modules |
| `DemoApplicationTests` | Context-load smoke test |

## Build pipeline

`./mvnw verify` runs, in order:

1. **sortpom** — keeps `pom.xml` tidy
2. **jMolecules ByteBuddy plugin** — transforms aggregates at compile time to generate JPA mappings (no `@Entity` in your code)
3. **Spotify fmt (Google Java Format)** — `check` goal, fails on unformatted code (`mvn fmt:format` to auto-fix)
4. **Checkstyle** — uses custom `checkstyle.xml` modeled on Google's config; errors fail the build, Javadoc warnings print but don't fail
5. **Tests** — including the module/DDD-rule enforcement tests above
6. **Spring Boot repackage** — runnable jar

## Key decisions

- **Feature-first package layout** — each module contains its own layer stack.
- **Application service is the module's public API.** Other modules call `CustomerService.exists(id)` instead of maintaining their own customer projection. Published language > projection duplication.
- **Domain events for writes, published use-cases for reads.** Avoids eventual-consistency gymnastics for simple existence checks while keeping write-side flows fully async and decoupled.
- **Application services return `*View` records, not domain aggregates.** Controllers map View → DTO. Aggregates never leave the application layer.
- **Repository interfaces live in `domain/`** (extending Spring Data's `CrudRepository`). Pragmatic coupling to Spring Data beats duplicating a bridge class per aggregate.
- **Flyway owns the schema, Hibernate doesn't** (`ddl-auto=none`). The test DB and prod DB run the same migrations.

## Further reading

- [DDD.md](DDD.md) — the pattern decisions (layering, commands/views/DTOs, domain vs application services, port/adapter)
- [JMOLECULES.md](JMOLECULES.md) — jMolecules vocabulary and the DDD tactical rules it enforces
- [MODULITH.md](MODULITH.md) — Spring Modulith module boundaries, events, named interfaces
- [CLAUDE.md](CLAUDE.md) — global rules Claude Code loads every session
- [CLAUDE_CODE.md](CLAUDE_CODE.md) — the architecture: four golden rules, PIV loop, commands, agents, PRPs
- [USAGE.md](USAGE.md) — hands-on playbook: scenarios, INITIAL.md checklist, cheatsheet, troubleshooting
