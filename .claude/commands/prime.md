---
description: Prime the session with this repo's rules, docs, module tree, and recent git log.
argument-hint: (none)
---

<role>
You are starting or resuming work on a Spring 4 + DDD + Spring Modulith + jMolecules repository.
Your job right now is to load the repo's rules, architecture, and recent state into working
memory — not to write code, not to answer questions. Just prime.
</role>

<golden_rules_in_play>
- **Context reset.** After this command, context is the minimum needed. Don't pull more unless a specific task requires it.
- **Command defines everything.** These instructions are the priming procedure — nothing else required.
- **Git log as memory.** Step 4 below is how state is recovered across sessions.
- **System evolution.** If this output feels stale or wrong, run `/evolve "prime missed <X>"` later.
</golden_rules_in_play>

<workflow>

<step n="1" name="load rules">
Read in full:
- `CLAUDE.md` — global rules (kept concise)
- `CLAUDE_CODE.md` — the Claude Code workflow, four golden rules, PIV loop, command and skill catalog
</step>

<step n="2" name="scan architecture docs">
Scan *headings only* of each, then fetch relevant sections only if recent activity hints at their topic:
- `README.md` — bird's-eye project overview
- `DDD.md` — layering + "where each concern lives" table
- `MODULITH.md` — module boundaries, `@NamedInterface`, events
- `JMOLECULES.md` — DDD rules enforced by tests
- `USAGE.md` — scenario walkthroughs and cheatsheet
</step>

<step n="3" name="inspect module tree">
```bash
ls src/main/java/com/example/demo/
ls src/main/java/com/example/demo/customers/
ls src/main/java/com/example/demo/catalog/
ls src/main/java/com/example/demo/inventory/
ls src/main/java/com/example/demo/orders/
```

Confirm the four bounded-context modules exist, each with `domain/`, `application/`, and
optional `infrastructure/`/`web/` sub-packages.
</step>

<step n="4" name="recover state from git">
```bash
git log --oneline -20 2>/dev/null || echo "Not a git repository — 'git log as memory' rule inactive. Run 'git init' to enable."
```

If git exists, skim the last ~20 commits for:
- Recent PRP executions (`feat(<module>): PRP-<slug> task N — …`)
- Open work (PRPs not capped with `PRP-<slug> complete`)
- Recent `evolve:` commits touching `.claude/` or `CLAUDE.md`

If git is absent, note it and move on.
</step>

<step n="5" name="check pending work">
```bash
ls PRPs/ 2>/dev/null
cat INITIAL.md 2>/dev/null | head -30
```

Non-template content in `INITIAL.md` → a feature is queued.
Files in `PRPs/` beyond template + examples → in-flight or reference PRPs.
</step>

<step n="6" name="emit summary">
Output exactly these lines (≤ 15 total):

- Modules visible: `<list>`
- Last activity (from git log): `<1-2 lines, or "no git">`
- Pending work (INITIAL.md / PRPs): `<summary, or "none">`
- Quality gate: `./mvnw verify`
- Next suggested command: one of `/generate-prp`, `/execute-prp`, `/verify`, `/evolve`, or "ready for ad-hoc exploration"
</step>

</workflow>

<rules>
- Don't read files beyond what the workflow names.
- Don't start any non-priming task. If the user needs code written, they'll ask next.
- Don't paraphrase the docs — you summarized them for yourself; the user doesn't need the summary.
</rules>
