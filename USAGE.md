# Using Claude Code on this repo — the playbook

Hands-on companion to [CLAUDE_CODE.md](CLAUDE_CODE.md). That doc explains *what* the architecture
is. This one shows *how to use it* day-to-day.

If you've never used the setup before, start at "First-time setup." If you're in the middle of a
feature, jump to "Daily scenarios."

---

## First-time setup

### 1. Install prerequisites

- [Claude Code](https://claude.com/claude-code) (CLI, desktop, or IDE extension)
- Java 25, Docker running
- Repo cloned locally

### 2. Initialize git (enables "git log as memory")

```bash
cd /Users/agust/Documents/mine/demo
git init
git add -A
git commit -m "initial commit"
```

Without git, `/prime` can't reconstruct state from commits and `/execute-prp` can't record its
own progress. The workflow *works* without git, but loses one of the four golden rules.

### 3. Verify the repo is green

```bash
./mvnw verify
```

Must end in `BUILD SUCCESS`. If it doesn't, the slash commands inherit that broken state — fix
first, work later.

### 4. Open a session and prime

In a fresh Claude Code session:

```
/prime
```

You should see a short summary: four modules listed, verify pipeline stages named, recent git
log (or "no prior commits"), next suggested command.

You're ready.

---

## Daily scenarios

### Scenario A — small change to existing code

> "Add a `GET /health` endpoint that returns `{"status":"up"}`."

For trivial changes, PRPs are overkill. Just:

1. `/prime` (always, at session start)
2. Tell Claude directly: _"Add a `GET /health` endpoint under a new `health/web/HealthController.java`. Return `{\"status\":\"up\"}`. Follow the controller conventions from `orders/web/OrdersController.java`."_
3. Claude implements + runs the relevant tests
4. `./mvnw verify` to confirm

Rule of thumb: if you can describe the change in one sentence and touch ≤ 3 files, skip the PRP.

### Scenario B — adding a feature (the standard PRP flow)

> "Add a loyalty-points calculation: every completed order earns (total / $10) points, stored against the customer."

This spans multiple modules — orders emits `OrderCompleted`, something listens, customers needs
a new field. Use the full PIV loop.

**Step 1 — open `INITIAL.md` and fill it in.** See the [INITIAL.md checklist](#writing-a-good-initialmd) below.

**Step 2 — generate the PRP:**
```
/prime
/generate-prp INITIAL.md
```
Output: `PRPs/loyalty-points.md`. Read it end-to-end. If a task is fuzzy or a success criterion
isn't testable, it's a bad PRP — fix it before continuing (edit the file by hand, or re-run
`/generate-prp` with a sharper `INITIAL.md`).

**Step 3 — reset context and execute:**
```
/clear
/execute-prp PRPs/loyalty-points.md
```
Claude executes task by task, commits at milestones. Watch the output. If a task halts with
"PRP says X but reality is Y", approve or edit, don't override silently.

**Step 4 — verify:**
```
/clear
/verify
```
Expect a layered report. Green across all six stages means done.

**Step 5 — if `/verify` is red,** jump to [Interpreting `/verify`](#interpreting-verify-output).

### Scenario C — adding a whole new module

> "Add a `notifications` module that reacts to events from other modules and sends messages."

Same as Scenario B. The existing `PRPs/EXAMPLE_notifications_module.md` is a worked example of
exactly this — read it before writing your `INITIAL.md` so you know the shape of a good PRP for
a whole-module addition.

Key differences from Scenario B:

- The architect will propose a fresh `<module>/` top-level package with the full four-layer
  split (`domain/ application/ infrastructure/ web/`).
- `package-info.java` files in `domain/` (and `application/` if cross-module reads are needed)
  must be `@NamedInterface` or Modulith's verify fails.
- A new `@ApplicationModuleTest` for the module is part of the PRP.

### Scenario D — you noticed a pattern of mistakes

> "The last three PRPs all forgot to mark `@NamedInterface` on new modules. The reviewer caught it each time. The rule isn't in `CLAUDE.md`."

This is what `/evolve` is for.

```
/evolve "generate-prp keeps producing PRPs that skip @NamedInterface on new module domain packages. Rule isn't anywhere visible to the architect agent."
```

`/evolve` identifies the right file to patch — in this case probably `.claude/agents/architect.md`
(where the architect agent's conventions live) — proposes the edit, applies it, and commits with
an `evolve:` prefix.

Next PRP for a new module automatically gets the `@NamedInterface` step. The system is strictly
better than it was an hour ago.

### Scenario E — resuming work a week later

> "I was in the middle of the loyalty-points feature last week. Where was I?"

```
/prime
```

Output includes recent git log. Look for:

- `feat(orders): PRP-loyalty-points task N — …` — you got through task N.
- No `feat(orders): PRP-loyalty-points complete` → still in flight.
- Any `evolve:` commits → rules may have changed since you started.

Check `PRPs/loyalty-points.md` — tasks marked `[x]` are done, `[ ]` are pending.

Then:

```
/execute-prp PRPs/loyalty-points.md
```

It picks up from the first unchecked task.

---

## Writing a good INITIAL.md

The four sections matter. Cole's rule: **if you leave a section empty, `/generate-prp` will
produce a bad PRP**.

### FEATURE

Describe:
- What you're building (the *what*)
- Where it lives (existing module or new)
- The success signal (what "done" looks like from the outside — endpoint response, event fired,
  log line, whatever)

**Bad:** "Add loyalty points."

**Good:** "On `OrderCompleted`, the orders module publishes a `LoyaltyPointsEarned(customerId, points)` event. Points = floor(totalInDollars / 10). The customers module listens and adds the points to a new `loyalty_points` column on the customer aggregate. A new `GET /customers/{id}/loyalty` endpoint returns the current balance."

### EXAMPLES

Existing code to pattern-match against:
- Similar patterns already implemented
- Style to imitate
- File paths

**Good:** "`inventory/infrastructure/InventoryListener.java` for the `@ApplicationModuleListener` shape. `orders/application/OrderService.java` for the application-service flow."

### DOCUMENTATION

External specs / docs the implementer will need. Skip if purely internal.

**Good:** "Spring Modulith event docs: https://docs.spring.io/spring-modulith/reference/events.html — specifically the 'Publishing events' section."

### OTHER CONSIDERATIONS

Constraints, non-functional requirements, gotchas you already know.

**Good:**
- "Points must be integer (no fractions)."
- "If the customer doesn't exist in the orders module's lookup, skip the points — don't error."
- "Must not break `EndToEndOrderFlowTests`."

### Checklist before `/generate-prp`

- [ ] FEATURE names a specific outcome, not a vague goal.
- [ ] EXAMPLES points to at least one real file in this repo.
- [ ] DOCUMENTATION includes URLs if external deps are involved.
- [ ] OTHER CONSIDERATIONS lists at least one concrete constraint.
- [ ] You could hand `INITIAL.md` to a stranger and they'd know what to build.

If any box is unchecked, `/generate-prp` will invent answers or ask clarifying questions — both
waste a turn. Fix `INITIAL.md` first.

---

## Reading a PRP before `/execute-prp`

Before you hit execute, skim the PRP and confirm:

- **Context matches your intent.** If the PRP drifted from what you asked for, stop and edit.
- **Success criteria are testable.** Vague criteria like "it works well" = bad PRP. Re-generate.
- **Every task has a verification command.** A task without a way to check it = bad PRP.
- **Architecture notes place files correctly.** Cross-reference `DDD.md`'s "where each concern lives" table.
- **Out of scope is explicit.** Prevents scope creep during implementation.

Don't hesitate to edit the PRP directly. It's a plain markdown file — fix typos, adjust tasks,
tighten criteria. The PRP is a human artifact until `/execute-prp` consumes it.

---

## Interpreting `/verify` output

The report names the **first** failing stage. Later stages skip.

| Failing stage | Quick fix |
|---|---|
| sortpom | `./mvnw sortpom:sort` then commit |
| ByteBuddy | Usually an aggregate missing `getId()` or Identifier not implementing `Identifier` — check [JMOLECULES.md](JMOLECULES.md) |
| fmt | `./mvnw fmt:format` then commit |
| Checkstyle | Read the violation; often naming or unused imports |
| ModulithStructureTests | A module imported another's non-public package. Read the message, remove the illegal import, or mark the target package `@NamedInterface` if it belongs in the public API |
| JMoleculesRulesTests | A DDD rule was violated. Common: direct aggregate reference instead of `Association<T, Id>`. Fix the aggregate |
| `@ApplicationModuleTest` | That module doesn't boot alone. Usually a dependency on another module's internal. Same fix as ModulithStructureTests |
| `EndToEndOrderFlowTests` | Async event chain broken. Start with the first `await()` that times out |
| `spring-boot:repackage` | Almost never fails if earlier stages passed |

`/verify` also surfaces **system gap candidates** at the bottom. If the failure matches a
pattern (same rule broken twice, same reviewer comment recurring), it'll suggest
`/evolve "<description>"`. Take the suggestion.

---

## Command cheatsheet

| Command | When | Notes |
|---|---|---|
| `/prime` | Start of every session | Always. Takes 5-10 seconds. |
| `/generate-prp <INITIAL.md>` | After filling INITIAL.md | Produces `PRPs/<slug>.md`. |
| `/clear` | Between phases | Drops accumulated chat. State lives in files, not conversation. |
| `/execute-prp <path>` | After reading the PRP | Commits per task. Resumable. |
| `/verify` | After executing a PRP | Layered report. Suggests system gaps. |
| `/evolve "<gap>"` | When a pattern of bugs appears | Edits exactly one file. |

Keyboard-speed approximation:

```
/prime
# write INITIAL.md
/generate-prp INITIAL.md
# read PRPs/<slug>.md, edit if needed
/clear
/execute-prp PRPs/<slug>.md
/clear
/verify
# if red: fix or /evolve, then loop
```

---

## Troubleshooting / FAQ

### "`/prime` says 'not a git repository'"

Run `git init && git add -A && git commit -m "initial"` in the repo root. The "git log as memory"
golden rule needs git.

### "`/generate-prp` asks me 10 questions I can't answer"

Your `INITIAL.md` is incomplete. The questions reveal which section is thin. Answer them *in
`INITIAL.md`*, then re-run `/generate-prp`. This way the next session sees the answers too.

### "`/execute-prp` stopped with 'PRP says X but reality is Y'"

The PRP was wrong. Either:
- Fix the PRP (edit the markdown) and re-run `/execute-prp`.
- Fix the reality (code outside the PRP was wrong — out of scope, but you can address it manually).

Don't tell Claude "just figure it out." That silently diverges the implementation from the PRP.

### "`/verify` is red and I don't know which stage"

Scroll up in the output. The first `[ERROR]` or `BUILD FAILURE` marker names the stage. The
layered report at the end summarizes.

### "I want to do something the PRP doesn't mention"

That's scope creep. Two options:
- Ignore it. Ship what the PRP covers. Write a new `INITIAL.md` for the extra thing later.
- Stop, update the PRP with the new task + verification, then continue.

Never add code the PRP doesn't authorize. It breaks the reproducibility property.

### "Context is full / responses feel confused"

`/clear`. This is normal — context rot is real. Every phase boundary is a natural reset point.

### "A PRP task modifies the same file the next task needs"

That's fine — sequential tasks can compound edits on the same file. The verification per task
keeps the file valid at each step.

### "I edited `CLAUDE.md` / a command / an agent by hand"

Fine, but commit it with an `evolve:` prefix so `git log` shows system-level changes separately
from feature work:

```bash
git add .claude/agents/implementer.md
git commit -m "evolve: implementer now reminds to run fmt:format after Java edits"
```

### "Two PRPs conflict or cover the same area"

PRPs are immutable after merging. If requirements change, write a new PRP that supersedes. Close
the old one with a task: `[x] 0. Superseded by PRPs/new-feature.md`.

### "I want to skip `/generate-prp` and just prompt directly"

You can — see [Scenario A](#scenario-a--small-change-to-existing-code). But for anything that
touches multiple files or modules, you'll regret it. The PRP is the artifact that makes the
change reproducible and reviewable.

---

## Anti-patterns (things that break the system)

1. **Long-running sessions across phases.** Use `/clear` aggressively. Your plan, implementation,
   and verify should each start cold.
2. **Hand-editing code during `/execute-prp`.** Let the tool run. Intervene only when the PRP is
   wrong, and fix the PRP, not the code directly.
3. **Adding rules to `CLAUDE.md` for every bug.** CLAUDE.md is a context budget, not a dumping
   ground. `/evolve` decides whether a fix belongs in CLAUDE.md, a command, or an agent.
4. **Running `/execute-prp` without reading the PRP first.** The PRP is the contract. If you
   haven't read it, you haven't consented to it.
5. **Ignoring `/verify`'s "system gap candidates" line.** That's the compounding-improvements
   loop. Skip it once, fine. Skip it every time, and the system stops evolving.
6. **Writing `INITIAL.md` vaguely and hoping `/generate-prp` figures it out.** It doesn't. It
   writes a vague PRP that produces vague code.

---

## Learning path

If you're new to this architecture:

1. Read [CLAUDE_CODE.md](CLAUDE_CODE.md) — understand the theory (10 min)
2. Read this doc — understand the practice (15 min)
3. Read [`PRPs/EXAMPLE_notifications_module.md`](PRPs/EXAMPLE_notifications_module.md) — see a real PRP (10 min)
4. Make a trivial change using Scenario A — muscle memory
5. Make a real feature change using Scenario B — the full loop

By the time you've run the full loop once, the rest is repetition.
