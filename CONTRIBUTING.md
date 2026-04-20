# Contributing

Thanks for your interest. This project is a Spring Boot 4 + Java 25 modular monolith
practicing tactical DDD with Spring Modulith and jMolecules. Before sending a change,
read the core docs:

- [CLAUDE.md](CLAUDE.md) — cross-cutting invariants and quality gate
- [DDD.md](DDD.md) — layering, commands/views/DTOs, ports and adapters
- [MODULITH.md](MODULITH.md) — module boundaries and event-driven comms
- [JMOLECULES.md](JMOLECULES.md) — aggregate/VO/event vocabulary
- [USAGE.md](USAGE.md) — hands-on playbook and troubleshooting
- [CLAUDE_CODE.md](CLAUDE_CODE.md) — PRP workflow

## Getting started

```bash
./mvnw verify          # full quality gate (must be green)
./mvnw spring-boot:run # run the app (brings up Postgres via docker compose)
```

## Workflow

1. Fill in `INITIAL.md` describing the feature.
2. Generate a PRP: `/generate-prp INITIAL.md`.
3. Execute it: `/execute-prp PRPs/<feature>.md`.
4. Verify: `/verify` (runs `./mvnw verify`).
5. Open a PR from a feature branch — do not push directly to `main`.

## Ground rules

- **Feature-first package layout.** Top-level package is the bounded context,
  not a technical layer. Never `application/<module>/…`.
- **Cross-module writes go through events.** Cross-module reads go through the
  target module's published application service — not via its projections or
  repositories.
- **Do not reach into another module's `infrastructure/` or `web/`** — those
  packages are internal.
- **Flyway owns schema.** `ddl-auto=none`; add migrations under
  `src/main/resources/db/migration/`.
- **Style:** Google Java Format (`./mvnw fmt:format`), Checkstyle clean.

## Quality gate

`./mvnw verify` runs all six stages and must pass before a PR is merged:

1. `sortpom` verify
2. jMolecules ByteBuddy transformation
3. Spotify `fmt:check` (Google Java Format)
4. Checkstyle (`checkstyle.xml`)
5. Tests — including `ModulithStructureTests` and `JMoleculesRulesTests`
6. Spring Boot repackage

## Commits and PRs

- Write focused commits with a clear subject line.
- Reference the PRP or issue the change addresses, if any.
- Keep PRs scoped to a single feature or fix. Refactors and unrelated cleanups
  belong in separate PRs.
- Do not skip hooks, do not force-push to `main`, do not `reset --hard`
  on shared branches.

## Reporting issues

Open a GitHub issue with:

- What you expected to happen.
- What actually happened (with the relevant `./mvnw verify` output if a test
  or architectural rule failed).
- Minimal steps to reproduce.
