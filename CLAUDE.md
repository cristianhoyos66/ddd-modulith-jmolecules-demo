# Global rules for this repo

Spring Boot 4 + Java 25 modular monolith practicing tactical DDD. Four bounded contexts
(`customers`, `catalog`, `inventory`, `orders`) in one codebase, isolated by Spring Modulith.

**Read these docs before non-trivial work:**

- [DDD.md](DDD.md) — layering, commands/views/DTOs, domain vs application services, port/adapter
- [MODULITH.md](MODULITH.md) — module boundaries, `@NamedInterface`, event-driven comms
- [JMOLECULES.md](JMOLECULES.md) — aggregate/VO/event vocabulary, ArchUnit rules enforced
- [CLAUDE_CODE.md](CLAUDE_CODE.md) — Claude Code workflow, four golden rules, PIV loop
- [USAGE.md](USAGE.md) — hands-on playbook with scenarios, cheatsheet, troubleshooting

**Layer-specific rules load automatically** when you open files in that layer (see
`.claude/rules/*.md`). This file stays concise and covers only cross-cutting concerns.

## Tech stack (do not change without a PRP)

- Java 25, Spring Boot 4.0.x, Spring Modulith, jMolecules
- Postgres (runtime, via `compose.yaml`), H2 (tests)
- Flyway owns schema (`ddl-auto=none`); migrations in `src/main/resources/db/migration/`
- Maven (`./mvnw`); virtual threads enabled

## Package layout (non-negotiable)

```
com.example.demo.<module>/
├── domain/           ← aggregates, VOs, events, repository interfaces (NamedInterface)
├── application/      ← services (use cases), commands, views (NamedInterface where called cross-module)
├── infrastructure/   ← listeners, external adapters (ports' implementations) — internal
└── web/              ← controllers, request/response DTOs — internal
```

Feature-first (module at top). **Never** layer-first (`application/customers/…`) — it breaks Modulith.

## Cross-cutting invariants

Four that apply regardless of which layer you're in:

- **Feature-first package layout.** Top-level = bounded context, not a technical layer.
- **Writes cross modules via events**, reads cross modules via the target module's **published application service** (not via projections).
- **`./mvnw verify` is the quality gate.** Any change must leave all six stages green.
- **Don't reach into another module's `infrastructure/` or `web/`** — those are internal.

Layer-specific invariants (aggregate shape, controller DTOs, listener patterns, etc.) live in
path-scoped rules under `.claude/rules/` and load on-demand when Claude works on that layer.

## Build & test (the quality gate)

```bash
./mvnw verify          # Full pipeline: sortpom → byte-buddy → fmt check → checkstyle → tests → repackage
./mvnw test            # Tests only
./mvnw fmt:format      # Auto-fix formatting
./mvnw sortpom:sort    # Auto-fix pom.xml ordering
./mvnw spring-boot:run # Start app (docker-compose brings up Postgres)
```

Six verify stages:

1. sortpom (verify goal)
2. jMolecules ByteBuddy transformation
3. Spotify fmt check (Google Java Format)
4. Checkstyle (`checkstyle.xml`)
5. Tests — including `ModulithStructureTests` (Modulith verify) and `JMoleculesRulesTests` (DDD rules)
6. Spring Boot repackage

## When to do what

- Starting a session → `/prime`
- Have a feature to build → fill `INITIAL.md` → `/generate-prp INITIAL.md`
- Ready to code → `/execute-prp PRPs/<feature>.md`
- Check it's done → `/verify`
- Found a recurring gap → `/evolve "<description>"`

## Style

- Google Java Format (enforced by `fmt:check`). Run `mvn fmt:format` to auto-fix.
- Javadoc recommended on public domain types; Checkstyle warns but doesn't fail.
- Don't comment the obvious. Do comment non-obvious constraints.

## Safety

- Don't commit unless explicitly asked.
- Don't push, don't `reset --hard`, don't skip hooks.
- When in doubt, consult the docs at the top of this file.
