---
name: sdd
description: "Use when the user wants to run the full Spec-Driven Development pipeline: Jira issue to spec, implementation, code review, test generation, and test review. Triggered by 'sdd', 'spec driven development', or a Jira issue key like SVY-12345."
---

# SDD — Spec-Driven Development Pipeline (opencode)

You are the **orchestrator** for the full SDD pipeline:
PM Agent → Coding → Code Review → Test Gen → Test Review → Commit.

You collect output from each phase, show summaries to the user at approval gates,
and thread context forward **selectively** to maintain isolation between phases.

## Context isolation principle

Each phase runs as a `task` subagent with a **fresh context**. This prevents bias:
- The Coder only sees the spec, not the PM's internal analysis
- The Code Reviewer only sees the spec + actual code, not the Coder's reasoning
- The Test Generator only sees the spec + implementation, not review findings

You control exactly what information flows between phases via the `task` prompt.

## Input

The user provides a Jira issue key or URL, optionally followed by extra context, e.g.:
`SVY-21080 some text meant to give more context about the case`

Parse the first token as the issue key/URL. Everything after it is supplementary
context provided by the user to clarify or augment the Jira ticket.

Record the issue key as `ISSUE_KEY` and the extra text (if any) as `USER_CONTEXT`.

---

## Phase 1 — PM Agent: Jira → Spec

Read the file `.opencode/skills/sdd/phases/pm-agent.md` and pass its full content
as instructions in a `task` prompt:

```
task(subagent_type='general', prompt="""
<contents of phases/pm-agent.md>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None" if the user provided no extra text)
""")
```

The task's output will be the relative path to the spec file it created.
Record that as `SPEC_PATH`.

**HUMAN GATE — Spec approval**

Use the `question` tool:
- Header: "Spec Review"
- Question: "Phase 1 complete. Spec written at `SPEC_PATH`. Please review it, then choose:"
- Options:
  - "Approve" — proceed to implementation
  - "Request changes" — provide feedback and I'll revise

If the user requests changes, apply edits yourself for minor revisions. For
substantial rewrites, spawn a `task(subagent_type='general')` with the feedback.

Loop until approved.

---

## Phase 2 — Coding: Spec → Implementation

Read `.opencode/skills/sdd/phases/coding.md` AND `.opencode/skills/sdd/phases/project-context.md`,
then spawn:

```
task(subagent_type='general', prompt="""
<contents of phases/project-context.md>

---

<contents of phases/coding.md>

Spec file to implement: SPEC_PATH
""")
```

**Important:** Pass the project context + coding instructions + spec path.
Do NOT include the PM agent's analysis, code samples, or reasoning — the coder
should form their own implementation approach based solely on the spec.

Record the returned file list as `CHANGED_FILES`.

---

## Phase 3 — Code Review

Read `.opencode/skills/sdd/phases/code-review.md` and spawn:

```
task(subagent_type='general', prompt="""
<contents of phases/code-review.md>

Spec file: SPEC_PATH
""")
```

**Important:** Pass ONLY the spec path. The reviewer must look at the actual code
via git diff and source reading tools — not be influenced by the coder's context.

The task's response must begin with `APPROVED` or `CHANGES NEEDED`.

**If `CHANGES NEEDED`:**

Use the `question` tool:
- Options:
  - "Auto-fix" — spawn a coding agent with the review findings
  - "I'll fix manually" — pause until user says to continue
  - "Override" — proceed despite findings

If **Auto-fix**: spawn a `task(subagent_type='general')` with:
- The project context (`phases/project-context.md`)
- The spec path
- The review findings (these are explicitly passed — they're the "contract" for the fix)
- Instructions from `phases/coding.md`

Then re-run Phase 3. Repeat until `APPROVED` or user overrides.

---

## Phase 4 — Test Generation

Read `.opencode/skills/sdd/phases/test-gen.md` AND `.opencode/skills/sdd/phases/project-context.md`,
then spawn:

```
task(subagent_type='general', prompt="""
<contents of phases/project-context.md>

---

<contents of phases/test-gen.md>

Spec file: SPEC_PATH
""")
```

Record the output as `TEST_FILES`.

---

## Phase 5 — Test Review

Read `.opencode/skills/sdd/phases/test-review.md` and spawn:

```
task(subagent_type='general', prompt="""
<contents of phases/test-review.md>

Spec file: SPEC_PATH
""")
```

Response must begin with `APPROVED` or `CHANGES NEEDED`.

**If `CHANGES NEEDED`:** spawn a fix agent with review findings + spec path,
then re-run Phase 5. Repeat until `APPROVED`.

---

## Phase 6 — Commit

**HUMAN GATE — Final approval**

Use the `question` tool:
- Header: "Ready to commit"
- Question: "All phases complete. Spec: `SPEC_PATH`, Implementation: `CHANGED_FILES`, Tests: `TEST_FILES`. Ready to commit?"
- Options:
  - "Commit now"
  - "Let me review first"

When approved, use `eclipse-git_gitStatus` to see all changes. Stage every file
belonging to this feature with `eclipse-git_gitAdd`.

**Include:** feature code, spec file, test files, modified pom.xml, AGENTS.md (if test docs were added)
**Exclude:** opencode.json, .opencode/, unrelated files

Commit with `eclipse-git_gitCommit`. Message format:
```
<JIRA_KEY> <short description from spec title> [ai]

- bullet points summarising what was built

Co-Authored-By: opencode <noreply@opencode.ai>
```

After committing, display the full commit message in a formatted block.

---

## Error handling

- If any `task` returns a tool error, report it to the user and ask how to proceed.
- If the Atlassian MCP is unavailable, the PM phase will fall back to WebFetch.
- If a phase produces unexpected output, show it to the user via `question` tool.
