# Claude Code architecture for this repo

Context-engineering setup inspired by Cole Medin's approach
([`coleam00/context-engineering-intro`](https://github.com/coleam00/context-engineering-intro)).
The goal: stop ad-hoc prompting, stop drift, make the AI's workflow a first-class part of the
repo that's versioned, verifiable, and self-evolving.

> TL;DR. You don't prompt Claude directly for a feature. You fill an `INITIAL.md`, run
> `/generate-prp INITIAL.md` to produce a PRP (Product Requirement Prompt), then
> `/execute-prp PRPs/<feature>.md` to build it, then `/verify`. If something's off, run
> `/evolve` and the system improves itself.

## The four golden rules

1. **Reset the context.** Long-running conversations rot. Each phase (plan, implement, verify)
   runs in a **fresh session** with `/clear`. State is passed between phases through *files*
   (INITIAL.md, PRPs, git log) — never through accumulating chat.

2. **The command defines everything.** Each slash command (command or skill) in `.claude/` is
   self-contained: it loads its own context, names its own agents, and lists its own validation
   steps. You shouldn't have to brief Claude on how to run it.

3. **Git log as memory.** Commits made during `/execute-prp` are structured
   (`feat(<module>): <PRP task N> — <summary>`) so `/prime` can reconstruct "what we were doing"
   from `git log`. **Prerequisite:** the repo must be a git repo. Run `git init` once.

4. **System evolution mindset.** When a bug, missing rule, or repeated reviewer comment shows
   up, the fix is not "patch the code and move on." The fix is:
   a) patch the code, then
   b) `/evolve` → update the relevant rule, command, or agent so the class of error can't recur.
   Every bug is an opportunity to raise the floor.

## The PIV loop

```
  ┌───────────┐   /generate-prp   ┌───────────┐   /execute-prp   ┌───────────┐
  │INITIAL.md │  ───────────────▶ │ PRPs/*.md │ ───────────────▶ │  code +   │
  │ (human)   │                   │ (AI plan) │                  │  tests    │
  └───────────┘                   └───────────┘                  └─────┬─────┘
                                                                       │
                                                         /clear ◀──────┤
                                                                       ▼
                                                                  ┌─────────┐
                                                                  │ /verify │
                                                                  └────┬────┘
                                                                       │ gap?
                                                                       ▼
                                                                  ┌─────────┐
                                                                  │ /evolve │───▶ patches CLAUDE.md
                                                                  └─────────┘     or a command/agent
```

Between each box you `/clear`. The output file is all the next phase needs.

## Commands and skills

All five are invoked with `/<name>`. Claude Code lets you mix both mechanisms — they work
identically from the user side. The split here is about *what each one needs*:

**Command** (`.claude/commands/*.md`) — simple markdown file, explicit invocation, no
per-skill frontmatter needed.

**Skill** (`.claude/skills/<name>/SKILL.md`) — richer YAML frontmatter: `disable-model-invocation`
(prevents Claude auto-firing), `allowed-tools` (pre-approved per-skill tool list), `context: fork`
(run in an isolated subagent), and ability to bundle supporting files in the skill's directory.

| Invocation | Type | Path | Why |
|---|---|---|---|
| `/prime` | Command | `.claude/commands/prime.md` | Simple, explicit, read-heavy — no skill features needed |
| `/generate-prp <initial>` | Skill | `.claude/skills/generate-prp/SKILL.md` | Research-heavy; benefits from potential `context: fork` |
| `/execute-prp <prp>` | Skill | `.claude/skills/execute-prp/SKILL.md` | Stateful; `disable-model-invocation: true` + `allowed-tools` for `./mvnw *` and `git *` |
| `/verify` | Skill | `.claude/skills/verify/SKILL.md` | Pre-approves `./mvnw *` via `allowed-tools` |
| `/evolve <gap>` | Skill | `.claude/skills/evolve/SKILL.md` | Edits `.claude/` itself — `disable-model-invocation: true` is essential |

All prompt bodies use **XML notation** (`<role>`, `<workflow>`, `<rules>`, `<anti_patterns>`, …)
per [Anthropic's prompt engineering guidance](https://docs.anthropic.com/en/docs/build-with-claude/prompt-engineering/use-xml-tags).
XML structure makes the multi-section playbooks unambiguous for Claude to parse.

## Path-scoped rules (`.claude/rules/`)

Layer-specific rules live in `.claude/rules/*.md` with `paths` frontmatter. They load into
context only when Claude opens a file matching the pattern — so editing a controller loads the
web rules but not the migration rules.

| File | Scope (`paths:`) | Covers |
|---|---|---|
| `domain-layer.md` | `src/main/java/com/example/demo/*/domain/**/*.java` | Aggregates, VOs, events, repositories, domain services |
| `application-layer.md` | `src/main/java/com/example/demo/*/application/**/*.java` | Services, commands, views, ports, `@Transactional` |
| `infrastructure-layer.md` | `src/main/java/com/example/demo/*/infrastructure/**/*.java` | Listeners, adapters |
| `web-layer.md` | `src/main/java/com/example/demo/*/web/**/*.java` | Controllers, request/response DTOs |
| `tests.md` | `src/test/**/*.java` | `@ApplicationModuleTest`, Awaitility, architectural tests |
| `migrations.md` | `src/main/resources/db/migration/**/*.sql` | Flyway naming, append-only, portable SQL |
| `prps.md` | `PRPs/**/*.md` | PRP template conformance, task rules, naming |

This keeps `CLAUDE.md` slim (cross-cutting only) and puts layer-specific invariants next to the
files they govern — Claude sees them at the moment they matter.

## Subagents (`.claude/agents/`)

The heavy lifting inside commands and skills is delegated to specialized subagents:

| Agent | Model | Job |
|---|---|---|
| `architect` | opus | Design a blueprint before code is written. Used in `/generate-prp`. |
| `implementer` | sonnet | Write code per PRP tasks. Used in `/execute-prp`. |
| `reviewer` | opus | Audit the result against the PRP and `CLAUDE.md`. |
| `tester` | sonnet | Write/run `@ApplicationModuleTest`, integration, and scenario tests. |
| `researcher` | sonnet | Deep-dive on an unfamiliar pattern or library. |

Each agent has tool restrictions appropriate to its role (reviewer is read-only, implementer
can't WebFetch, etc.).

## PRPs (`PRPs/`)

A **PRP (Product Requirement Prompt)** is the durable plan document a feature produces. Think of
it as a design doc that's also the prompt — it contains enough context that `/execute-prp` can
build the feature from it alone, without the conversation that produced it.

Every PRP has these sections:

- **Context** — why this is being built, what problem it solves
- **Success criteria** — what "done" looks like, observable
- **Relevant existing code** — file paths + functions/patterns to reuse
- **Architecture notes** — which module/layer/package, which events, cross-module dependencies
- **Task list** — ordered, small, independently verifiable
- **Validation gates** — per-task and final commands to run
- **Error handling / risks** — failure modes, rollback, known gotchas

See `PRPs/templates/prp_base.md` for the template and `PRPs/EXAMPLE_notifications_module.md` for
a worked example.

## End-to-end workflow (example)

You want to add a `notifications` module that sends emails after an order is completed.

1. **Fresh session.** `/clear`.
2. **Prime.** `/prime` — Claude loads rules, docs, module tree, recent commits.
3. **Write the request.** Open `INITIAL.md`, fill in `FEATURE`, `EXAMPLES`, `DOCUMENTATION`, `OTHER CONSIDERATIONS`.
4. **Generate the PRP.** `/generate-prp INITIAL.md` — produces `PRPs/notifications-module.md`.
5. **Read the PRP.** Confirm it matches your intent. Edit if needed.
6. **`/clear`.** Fresh context.
7. **Execute.** `/execute-prp PRPs/notifications-module.md` — code + tests written, commits made.
8. **`/clear`.** Fresh context.
9. **Verify.** `/verify` — full pipeline run, layered report.
10. **If gap:** `/evolve "<what was missing>"` → fixes CLAUDE.md / a command / an agent. Commit.

Humans stay in charge of planning (the PRP) and validation (reading the verify report, manual testing). The AI handles the coding inside that frame.

## What NOT to do

- **Don't skip `/prime`** at session start and expect Claude to know the repo. Always prime.
- **Don't let the conversation span phases.** Context rot kills quality. `/clear` often.
- **Don't fix the same class of bug twice** without running `/evolve`. If two PRPs generated the same mistake, the system is broken, not the code.
- **Don't modify `.claude/` files by hand** for one-off tweaks. If a command needs to change, it needs to change for everyone — treat `.claude/` as code, not scratch notes.
- **Don't run `/execute-prp` without a PRP.** Ad-hoc prompting defeats the whole architecture.

## Prerequisites

- `git init` in the repo (enables "git log as memory").
- Docker running (for `spring-boot:run` + Postgres via compose).
- Claude Code with the `.claude/` folder committed.

## How to actually use this

See [USAGE.md](USAGE.md) — the hands-on playbook with concrete scenarios (small change, new
feature, new module, evolving the system, resuming work days later), an `INITIAL.md` checklist,
a `/verify` triage table, a command cheatsheet, and a troubleshooting FAQ.

## Why this is better than prompting directly

Ad-hoc prompting tops out around "help me add this thing." You lose:

- Reproducibility — the same request tomorrow produces different code.
- Reviewability — there's no artifact showing what was planned.
- Compounding improvements — errors don't teach the system anything.
- Parallel work — two people can't agree on conventions without a written rule.

PRPs + commands + agents give you a repeatable, auditable, self-improving loop. Over months, the
`.claude/` tree gets smarter because every bug makes the system better. That's the real payoff.
