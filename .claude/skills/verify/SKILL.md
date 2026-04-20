---
name: verify
description: Run the full quality gate (./mvnw verify) and report a layered pass/fail summary. Diagnoses the first failing stage and flags system gap candidates for /evolve. Use after implementing changes, or to confirm the repo is green.
disable-model-invocation: true
allowed-tools: Read Bash(./mvnw *) Bash(cat *) Bash(tail *) Bash(head *) Glob Grep
---

<role>
You are the quality gatekeeper. You run the full verify pipeline, name the first failing stage,
and suggest the right fix. You also spot patterns that indicate a system gap — input for `/evolve`.
</role>

<output>
A layered pass/fail report + suggested fix if red + optional "possible system gap" line.
</output>

<golden_rules_in_play>
- **System evolution.** Every failure is a signal. Flag recurring patterns for `/evolve`.
- **Command defines everything.** This skill knows every stage and its quick fix — no external lookup required.
</golden_rules_in_play>

<workflow>

<step n="1" name="run the full build">
```bash
./mvnw verify 2>&1 | tee /tmp/verify-output.log
```
</step>

<step n="2" name="parse stages">
The pipeline has six stages. Report each:

<stages>
<stage name="sortpom">pom.xml order (sortpom-maven-plugin:verify)</stage>
<stage name="byte-buddy">jMolecules JPA transformation</stage>
<stage name="fmt">Spotify fmt check — Google Java Format</stage>
<stage name="checkstyle">checkstyle.xml rules</stage>
<stage name="tests">Unit, module (@ApplicationModuleTest), architectural (ModulithStructureTests, JMoleculesRulesTests), scenario (EndToEndOrderFlowTests)</stage>
<stage name="repackage">Spring Boot fat jar</stage>
</stages>
</step>

<step n="3" name="diagnose first failure">
If a stage failed, identify the fix from this table:

<diagnosis>
<case stage="sortpom">Auto-fix: `./mvnw sortpom:sort` then commit.</case>
<case stage="byte-buddy">Usually an aggregate missing `getId()` or an Identifier not implementing `Identifier`. Check `JMOLECULES.md`.</case>
<case stage="fmt">Auto-fix: `./mvnw fmt:format` then commit.</case>
<case stage="checkstyle">Manual fix. Read the violation message; usually naming or unused imports.</case>
<case stage="tests" test="ModulithStructureTests">A module imported another's non-public package. Remove the illegal import, or mark the target package `@NamedInterface` if it belongs in the public API.</case>
<case stage="tests" test="JMoleculesRulesTests">A DDD rule was violated. Most common: direct aggregate-to-aggregate reference instead of `Association<T, Id>`. Read `JMOLECULES.md`.</case>
<case stage="tests" test="@ApplicationModuleTest">The module doesn't boot standalone. Usually a missing `@NamedInterface` or an illegal cross-module dependency.</case>
<case stage="tests" test="EndToEndOrderFlowTests">The event chain broke. Start with the first `await()` that times out — trace back to the listener that didn't fire.</case>
<case stage="repackage">Almost never fails if earlier stages passed. If it does, inspect the full log.</case>
</diagnosis>
</step>

<step n="4" name="emit layered report">
<output_format>
```
Verify summary — <date>
─────────────────────────
sortpom        <✓ | ✗>
ByteBuddy      <✓ | ✗>
fmt            <✓ | ✗ with details>
checkstyle     <✓ | ✗ | skipped>
tests          <✓ | ✗ with failing test class | skipped>
repackage      <✓ | ✗ | skipped>

First failure: <stage + short description>
Suggested fix: <command or guidance>
```

Always name the **first** failing stage — later stages skip.
</output_format>
</step>

<step n="5" name="detect system gaps">
Before exiting, check whether this failure matches a pattern:

<gap_triggers>
- Same bug class flagged by reviewer last time → possibly a rule missing from `CLAUDE.md`.
- Test suite caught something the implementer should have prevented → implementer subagent's prompt may be missing a reminder.
- PRP didn't mention this constraint → `generate-prp` may need a step added.
- `./mvnw verify` doesn't actually catch the regression → add a test, or update this skill's diagnosis table.
</gap_triggers>

If one matches, output a final line: `Possible system gap: <one-line description>. Consider: /evolve "<description>"`.

Don't fix the system gap here — that's `/evolve`'s job.
</step>

</workflow>

<rules>
- **Don't auto-run** `fmt:format` or `sortpom:sort` unless the user explicitly approves — suggest, don't apply.
- **Don't** re-run tests multiple times hoping for different results. Flakiness is itself a system gap — flag it.
- **Don't** try to fix the code during `/verify`. This skill diagnoses; fixes happen in the normal flow.
</rules>

<anti_patterns>
- Giving a generic "the build failed" summary without naming the first bad stage.
- Suggesting a fix that doesn't match the actual failure (e.g. "run sortpom:sort" for a Checkstyle violation).
- Ignoring the system-gap line — that's the compounding-improvement loop.
</anti_patterns>
