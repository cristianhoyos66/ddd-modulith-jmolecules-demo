---
name: generate-prp
description: Generate a Product Requirement Prompt (PRP) from a filled-out INITIAL.md feature request. Researches the codebase, delegates to architect + researcher subagents, produces a self-contained PRP in PRPs/<slug>.md the next session can execute without context. Use after the user has filled in INITIAL.md and wants a plan.
disable-model-invocation: true
argument-hint: <path to INITIAL.md>
---

<role>
You are the PRP author. You turn a human-written feature request into an executable blueprint
that a future session can implement blind — with zero chat context from this session.
</role>

<inputs>
$ARGUMENTS — path to a feature-request file (typically `INITIAL.md`).
</inputs>

<output>
A new file: `PRPs/<kebab-slug>.md`, built from `PRPs/templates/prp_base.md`.
</output>

<golden_rules_in_play>
- **Command defines everything.** By the end, the PRP must be fully self-contained. Assume the executor has never seen the feature request.
- **Context reset.** Do NOT start implementing. The whole point is to hand off via file.
</golden_rules_in_play>

<workflow>

<step n="1" name="load the request">
Read `$ARGUMENTS` in full. Identify the four sections:
- FEATURE — what's being built
- EXAMPLES — existing code to pattern-match against
- DOCUMENTATION — external specs / URLs
- OTHER CONSIDERATIONS — constraints, gotchas

If any section is missing or vague, **stop and ask the user**. Do not invent requirements.
</step>

<step n="2" name="research in parallel">
Launch the `researcher` subagent with up to three parallel prompts (single message, multiple tool calls) — examples:
- "Find patterns in this repo similar to `<feature>`. Grep for `@ApplicationModuleListener`, existing services, similar aggregates. Report file paths with line numbers."
- "Identify which module the feature belongs in (existing or new). Check `DDD.md` and `MODULITH.md` for placement rules."
- "List the `./mvnw verify` pipeline stages that apply, plus any architectural tests the change must still pass."

Gather all results before writing.
</step>

<step n="3" name="consult the architect">
Launch the `architect` subagent. Provide:
- The raw feature request
- The research findings from step 2
- Pointers to `DDD.md`, `MODULITH.md`, `JMOLECULES.md`

Ask for: module placement, layer per file, cross-module interactions (events vs published services), public-vs-internal decisions, new commands/views/DTOs, test strategy, risks, ordered task list.
</step>

<step n="4" name="write the PRP">
Copy `PRPs/templates/prp_base.md`. Fill every section:
- **Context** — often copy-pasted from FEATURE + OTHER CONSIDERATIONS
- **Success criteria** — observable, testable outcomes
- **Relevant existing code** — file paths *with line numbers*
- **Architecture notes** — the architect's blueprint distilled
- **Task list** — ordered, small, each independently verifiable with a single command
- **Validation gates** — always ends with `./mvnw verify`
- **Error handling / risks** — known failure modes, ArchUnit/Modulith catches
- **Out of scope** — explicitly excluded items
</step>

<step n="5" name="name and save">
Save to `PRPs/<kebab-feature-name>.md`. Name from the FEATURE line, not the INITIAL.md filename.
</step>

<step n="6" name="summarize and exit">
Emit three lines:
- Path to the PRP
- Number of tasks
- Suggested next: `/clear` → `/execute-prp PRPs/<feature>.md`

**Do not implement.** The next session runs the PRP cold.
</step>

</workflow>

<quality_bar>
A good PRP means:
- Every task fits in one mental chunk (≤ 5 files changed).
- Every task has a verification step that's a single command.
- No section says "figure out X" — if the architect didn't know, research more.
- A new team member (human or AI) can implement the feature from the PRP alone.
</quality_bar>

<anti_patterns>
- **Don't** paper over a vague feature request. Ask the human to flesh out INITIAL.md.
- **Don't** skip line numbers on "Relevant existing code". Pointers without line numbers waste the executor's time.
- **Don't** produce tasks whose verification command is "it should work" — pick a concrete test class or shell command.
- **Don't** start implementing. Your output is a file, not code.
</anti_patterns>

<rules>
- Delegate to `researcher` for file/doc searches — don't read everything yourself.
- Delegate to `architect` for the blueprint — don't design alone.
- One PRP per feature. Split large features into multiple PRPs at the planning boundary.
</rules>
