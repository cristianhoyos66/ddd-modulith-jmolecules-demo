---
paths:
  - "PRPs/**/*.md"
---

# Rules for PRPs

Loaded only when reading/writing files under `PRPs/`. Authoritative guidance in
[PRPs/README.md](../../PRPs/README.md) and [CLAUDE_CODE.md](../../CLAUDE_CODE.md).

## Template conformance

Every PRP follows the 8-section structure from `PRPs/templates/prp_base.md`:

1. Context
2. Success criteria
3. Relevant existing code
4. Architecture notes
5. Task list
6. Validation gates
7. Error handling / risks
8. Out of scope

If a PRP is missing a section, it's incomplete. The `/generate-prp` skill is responsible for
filling every section.

## Task list rules

- Tasks are ordered, numbered, and small (≤ 5 files changed).
- Each task has **one** verification command (a single test class or `./mvnw` invocation).
- Tasks use `[ ]` / `[x]` so progress is visible.
- Don't write a task whose verification is "it should work" — pick a concrete command.

## "Relevant existing code" rules

File references **must include line numbers** where possible:

```
Good: `customers/application/CustomerService.java:20-26` — the write-side pattern to imitate.
Bad:  `CustomerService` — the pattern to follow.
```

Without line numbers, the executor wastes time re-locating the reference.

## Naming

- File name: `<kebab-feature>.md` (e.g., `loyalty-points.md`, not `LoyaltyPoints.md` or `feature-42.md`).
- Name derived from the FEATURE line in the original `INITIAL.md`, not from the INITIAL filename.
- Example PRPs prefixed with `EXAMPLE_` (e.g., `EXAMPLE_notifications_module.md`) — these are
  reference material, not executable.

## Lifecycle

- A PRP is written once, executed once, then archived (stays in `PRPs/` as history).
- **Immutable after merge.** If requirements change, write a new PRP that supersedes. Close the
  old one with a task: `[x] 0. Superseded by PRPs/<new-feature>.md`.
- Don't re-run `/execute-prp` on a completed PRP. Write a new one.

## What a good PRP enables

A new team member (human or AI) can implement the feature from the PRP alone, without access
to the conversation that produced it. If the PRP needs clarification in chat, it's broken —
fix the PRP, not the conversation.

## Don't

- Don't leave "TBD" or "figure out X" in a PRP. Research happens during `/generate-prp`, not
  during `/execute-prp`.
- Don't include tasks without verification commands.
- Don't reference files without line numbers.
- Don't edit a PRP's tasks once execution has started (except to mark them `[x]`). Instead, if
  a task is wrong, stop execution and issue a delta report.
