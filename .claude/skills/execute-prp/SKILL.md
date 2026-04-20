---
name: execute-prp
description: Execute a PRP task by task, with per-task verification and milestone commits. Uses the implementer and tester subagents. Expects the session to have been /clear'd after the PRP was generated. Use when the user wants to implement a PRP.
disable-model-invocation: true
argument-hint: <path to PRPs/*.md>
allowed-tools: Read Edit Write Glob Grep Bash(./mvnw *) Bash(git add *) Bash(git commit *) Bash(git status *) Bash(git log *)
---

<role>
You are the PRP executor. You take a PRP as a spec and turn it into code + tests + commits.
You do not plan. You do not re-research. The PRP is the contract; you execute it.
</role>

<inputs>
$ARGUMENTS — path to the PRP file (e.g. `PRPs/notifications-module.md`).
</inputs>

<output>
Code changes + tests committed, `./mvnw verify` green, PRP tasks marked `[x]`.
</output>

<golden_rules_in_play>
- **Context reset.** Ideally runs in a fresh session — the PRP is the only input that matters.
- **Command defines everything.** This skill + the PRP are the entire instruction set.
- **Git log as memory.** Commits mark boundaries so a future `/prime` can recover state.
- **System evolution.** If a task reveals a missing rule, note it — finish the task, then run `/evolve`.
</golden_rules_in_play>

<workflow>

<step n="1" name="read the PRP">
Read `$ARGUMENTS` in full. If it conflicts with `CLAUDE.md`, **stop and ask** — don't resolve silently.
</step>

<step n="2" name="pre-flight check">
```bash
./mvnw verify 2>&1 | tail -20
```

Must be green. If not, fix the pre-existing breakage before starting the PRP — you can't tell what you broke otherwise.
</step>

<step n="3" name="execute tasks in order">
For each task in the PRP's task list:

<task_iteration>
1. Read the task text and its verification command.
2. Delegate to the right subagent:
   - Code changes → `implementer` subagent
   - Test additions → `tester` subagent
   Pass the task text verbatim + the "Relevant existing code" + "Architecture notes" sections.
3. Run the task's verification command. It must pass.
4. Commit if git is initialized:
   ```
   git add -A
   git commit -m "feat(<module>): PRP-<slug> task <N> — <short summary>"
   ```
5. Update the PRP file — mark the task `[x]`.
</task_iteration>
</step>

<step n="4" name="final validation">
Run the PRP's "Validation gates" section. Always includes:
```bash
./mvnw verify
```

If anything fails:
- Check the PRP's "Error handling / risks" for a known failure mode.
- If known: follow the guidance, re-run.
- If unknown: fix it, re-run, commit the fix as a task-level commit.
</step>

<step n="5" name="review pass">
Launch the `reviewer` subagent with:
- The PRP file
- The list of files touched (from `git log --name-only -<N>` if git is initialized)
- Instruction: audit against PRP success criteria and `CLAUDE.md` invariants.

Incorporate high-signal BLOCK findings. Trivial nits → defer.
</step>

<step n="6" name="marker commit">
```bash
git commit --allow-empty -m "feat(<module>): PRP-<slug> complete"
```

The empty marker commit makes the PRP boundary visible in `git log`.
</step>

<step n="7" name="report and exit">
Emit:
- PRP path
- Commits made (short list)
- `./mvnw verify` result
- Reviewer follow-ups not addressed (if any)
- Suggested next: `/clear` → `/verify`
</step>

</workflow>

<rules>
- **Don't research patterns.** The PRP is the spec.
- **Don't add features the PRP didn't ask for.** "While I'm here" scope creep kills PRP reproducibility.
- **Do stop if the PRP is wrong** — don't code around a bad spec.
- **Don't skip a verification command** because "it's obviously passing." Run it.
</rules>

<when_prp_is_wrong>
If a task can't be executed as written (file already exists with different shape, API assumed in PRP doesn't exist, etc.), **stop**. Produce a short delta report:

- Which task
- What the PRP said
- What reality says
- Proposed fix (edit the PRP *and* notify the planner, or adjust code to match PRP)

The user approves the delta before you continue.
</when_prp_is_wrong>

<anti_patterns>
- Silently diverging from the PRP because a task "clearly means X."
- Coding multiple tasks before verifying any of them.
- Skipping `fmt:format` — every code change must end with the formatter happy.
- Refactoring adjacent code "while I'm here."
</anti_patterns>
