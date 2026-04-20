---
name: evolve
description: System evolution. Take a description of what went wrong and route the fix to the right config file (CLAUDE.md, a skill in .claude/skills/, or a subagent in .claude/agents/). Propose exactly one edit per invocation. Use when the same class of bug appears twice or when /verify flags a "possible system gap".
disable-model-invocation: true
argument-hint: <description of the gap>
---

<role>
You are the system evolver. Every bug is an opportunity to raise the floor — not by patching
code, but by patching the rule/skill/agent that let the bug through. You route gaps to the
right config file and propose a single focused edit.
</role>

<inputs>
$ARGUMENTS — prose description of a gap. Examples:
- "Reviewer flagged direct aggregate references three times this week — the rule isn't in CLAUDE.md."
- "generate-prp produced a PRP missing a @NamedInterface task for a new module."
- "The tester agent keeps forgetting the @ApplicationModuleTest slicing convention."
</inputs>

<output>
One edit to exactly one file, committed with an `evolve:` prefix.
</output>

<golden_rules_in_play>
- **System evolution.** This skill IS the rule. Running it is the discipline.
- **Git log as memory.** `evolve:` prefix makes system-level changes distinguishable from feature work.
- **Command defines everything.** The whole evolve protocol lives in this file.
</golden_rules_in_play>

<workflow>

<step n="1" name="classify the gap">
Pick exactly one target file based on the gap's scope:

<routing>
<route target="CLAUDE.md">A rule every session should know (e.g., "don't put SQL in controllers") and no file states it.</route>
<route target=".claude/skills/<name>/SKILL.md">Same type of code keeps getting built wrong. The skill that produced it is deficient.</route>
<route target=".claude/commands/<name>.md">Same as above but for commands (e.g., `/prime`).</route>
<route target=".claude/agents/<name>.md">An agent's role is understated (e.g., reviewer never checks for lazy-loading).</route>
<route target="src/test/java/...">/verify doesn't catch a class of regression — add or extend a test.</route>
</routing>

<caveat>
**Don't default to CLAUDE.md.** Over-stuffing CLAUDE.md creates context rot. Only add there if
the rule must be in *every* session's context window. Otherwise it belongs in the specific skill
or agent that needs it.
</caveat>
</step>

<step n="2" name="propose the edit">
For the chosen file, produce:
- **Current state** — the relevant section, quoted.
- **Proposed change** — a diff-ready snippet.
- **Rationale** — why this file (one line).
- **Risk** — what the new rule might break (one line, often "nothing — tightens existing behavior").
</step>

<step n="3" name="dry-run against history">
Name one real past failure the proposed change would have intercepted. If you can't name one,
the gap isn't real — push back on the user.
</step>

<step n="4" name="apply">
Apply the edit with `Edit`. **Exactly one file, exactly one evolve.** If multiple files need
updating, that's two gaps — run `/evolve` twice.
</step>

<step n="5" name="commit">
```bash
git add <file>
git commit -m "evolve: <one-line summary> (closes gap: <short description>)"
```

The `evolve:` prefix and `closes gap:` tag make system changes visible to future `/prime` output.
</step>

<step n="6" name="verify the meta-change">
Sanity-check the edit:
<checks>
- If CLAUDE.md: still ≤ ~150 lines (context budget).
- If a skill: run the skill against a small test case.
- If an agent: mention next-use validation is deferred to the next PRP.
</checks>
</step>

</workflow>

<anti_patterns>
- **Adding to CLAUDE.md for every bug.** CLAUDE.md is a budget, not a junk drawer.
- **Editing code AND the system in one evolve.** Fix code first (if urgent), then run `/evolve` separately.
- **Multiple gaps in one evolve.** Split them.
- **Evolving based on a hypothetical.** Gaps need a concrete example.
</anti_patterns>

<rules>
- One gap, one file, one edit, one commit.
- The edit must be reviewable — if the proposed change is large, the gap is actually multiple gaps.
- If the edit would delete or weaken an existing rule, stop and ask the user.
</rules>
