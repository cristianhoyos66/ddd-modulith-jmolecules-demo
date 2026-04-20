# PRPs — Product Requirement Prompts

A PRP is the durable plan for a feature. It's the output of `/generate-prp` and the input to
`/execute-prp`. Think of it as a design doc that also happens to be the complete prompt the
next session needs — no prior context required.

## Flow

```
INITIAL.md  ──▶  /generate-prp  ──▶  PRPs/<feature>.md  ──▶  /execute-prp  ──▶  code + tests
```

Between `/generate-prp` and `/execute-prp` you run `/clear` to drop accumulated context.
That's the point of the PRP: it carries all the state across the reset.

## Naming

- Use kebab-case: `notifications-module.md`, `order-discount-rules.md`, `health-endpoint.md`.
- Derive the name from the FEATURE line, not from `INITIAL.md`'s filename.
- Completed PRPs stay in `PRPs/` — they're the project's design history.

## Structure

Every PRP follows `templates/prp_base.md`. Sections:

1. **Context** — why this exists
2. **Success criteria** — observable "done"
3. **Relevant existing code** — file paths + patterns to reuse
4. **Architecture notes** — module/layer decisions
5. **Task list** — ordered, each independently verifiable, with a verification command
6. **Validation gates** — final checks (always `./mvnw verify`)
7. **Error handling / risks** — known failure modes
8. **Out of scope** — what this PRP explicitly doesn't do

## Conventions

- **Tasks are small.** ≤ 5 files changed, one clear verification command. If a task is too big to summarize, it's too big — split it.
- **Every success criterion has a test.** If you can't test it, you can't know it's done.
- **No ambiguity.** "Figure out how to X" is not a task — the architect does that during `/generate-prp`.
- **Tasks reference files by path + line number** where possible. "Extend OrderService.java around line 42 with a new `calculateDiscount()` method" is good. "Add discount logic" is not.

## Templates and examples

- `templates/prp_base.md` — the blank template
- `EXAMPLE_notifications_module.md` — a worked example for this repo's conventions

## Lifecycle

- A PRP is written once, executed once, then becomes archive. Treat it as immutable after
  merging (just like a commit). If requirements change, write a new PRP that supersedes it.
- The `git log` surface for a completed PRP is a series of `feat(<module>): PRP-<slug> task N` commits, capped with `feat(<module>): PRP-<slug> complete`.
