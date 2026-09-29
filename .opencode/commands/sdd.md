---
description: Run the Spec-Driven Development pipeline (Jira -> triage -> spec -> code -> review -> tests) for this Java / Eclipse-OSGi repo.
agent: build
---

Run the full Spec-Driven Development pipeline for this repository.

This repo is a **Java / Eclipse-OSGi** project, so load and follow the **sdd-java-eclipse**
skill (call the `skill` tool with id `sdd-java-eclipse`). That skill is the orchestrator; it
defines every phase and the human approval gates.

As the skill's `PROJECT_CONTEXT`, read the repository-local file
`.opencode/sdd/project-context.md` (relative to the current working directory) with the
`read` tool. Pass its full contents into every phase subagent — the phase subagents start
with fresh context and cannot see the repo otherwise. If that file is missing, tell the user
this repo has not been onboarded to SDD and stop.

User input (Jira issue key/URL, optionally followed by extra context): $ARGUMENTS