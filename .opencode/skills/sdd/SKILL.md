---
name: sdd
description: "Use when the user wants to run the full Spec-Driven Development pipeline: Jira issue to spec, implementation, code review, test generation, and test review. Triggered by 'sdd', 'spec driven development', or a Jira issue key like SVY-12345."
---

# SDD — Spec-Driven Development Pipeline (opencode)

You are the **orchestrator** for the full SDD pipeline:
Triage → PM Agent → Coding → Code Review → Test Gen → Test Review → Commit.

You collect output from each phase, show summaries to the user at approval gates,
and thread context forward **selectively** to maintain isolation between phases.

## Context isolation principle

Each phase runs as a `task` subagent with a **fresh context**. This prevents bias:
- The Triage agent evaluates the problem free of any spec-writing incentive
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

## Phase 0 — Triage & Root-Cause Investigation

Before writing any spec, run an isolated triage agent whose only mandate is to find
the *actual* root cause and decide whether — and how — the problem should be addressed.
This prevents the pipeline from taking the ticket's proposed solution at face value.

Read the file `.opencode/skills/sdd/phases/triage.md` and pass its full content as
instructions in a `task` prompt:

```
task(subagent_type='general', prompt="""
<contents of phases/triage.md>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None" if the user provided no extra text)
""")
```

**Important:** Pass ONLY the issue key + user context. The triage agent must investigate
the codebase and git history itself and form an independent judgement — do not hand it a
proposed solution.

The task's output will be the relative path to the triage report it created. Record that
as `TRIAGE_PATH`. Read the report to obtain its **verdict** (`PROCEED`, `NO_ACTION`, or
`NEEDS_INPUT`) and recommendation.

**HUMAN GATE — Triage decision**

Display a short summary of the triage report (verdict + recommendation), then present
the appropriate gate based on the verdict:

**The AI recommends, the human decides.** Never auto-stop on `NO_ACTION` — always route
through a gate and let the user confirm.

### Gate for `PROCEED` verdict

Use the `question` tool:
- Header: "Triage Decision"
- Question: "Phase 0 complete. Triage report at `TRIAGE_PATH` (verdict: PROCEED). Please review it, then choose how to proceed:"
- Options:
  - "Proceed to spec" — accept the recommended approach and run the PM Agent
  - "No action — stop pipeline" — end the pipeline; nothing further is generated
  - "Redirect approach" — provide a different direction; I'll run the PM Agent with that

Handle the choice:
- **"Proceed to spec"** — record the recommended approach from the report as
  `APPROVED_APPROACH` and continue to Phase 1.
- **"No action — stop pipeline"** — inform the user the pipeline has ended with no changes
  and stop. Do not run any further phase.
- **"Redirect approach"** — record the user's direction as `APPROVED_APPROACH` and continue
  to Phase 1.

### Gate for `NO_ACTION` verdict

Use the `question` tool:
- Header: "Triage Decision"
- Question: "Phase 0 complete. Triage report at `TRIAGE_PATH` (verdict: NO_ACTION — triage recommends no code change). Please review it, then choose how to proceed:"
- Options:
  - "No action — stop pipeline" — agree with triage; nothing further is generated
  - "Redirect approach" — override triage and provide a direction for the spec

Handle the choice:
- **"No action — stop pipeline"** — inform the user the pipeline has ended with no changes
  and stop. Do not run any further phase.
- **"Redirect approach"** — record the user's direction as `APPROVED_APPROACH` and continue
  to Phase 1.

### Gate for `NEEDS_INPUT` verdict

The report contains a "Questions for the reporter" section with the specific information
needed. Present these to the user via the `question` tool:

- Header: "Missing Information"
- Question: "Triage needs the following information before a spec can be written:\n\n<numbered list from the report's 'Questions for the reporter' section>\n\nHow would you like to proceed?"
- Options:
  - "Answer here" — I'll provide the answers now
  - "Post questions to Jira" — post these questions as a comment on the case
  - "Stop pipeline" — end here

Handle the choice:
- **"Answer here"** — ask the user for answers (via the `question` tool), record
  their answers as `USER_CONTEXT` additions, then present the **post-answer gate**
  below. The answers feed **forward** into the PM Agent — do **not** loop back into
  Triage.
- **"Post questions to Jira"** — follow the Jira comment posting flow below, then
  inform the user the pipeline is paused pending a reply on the ticket.
- **"Stop pipeline"** — inform the user the pipeline has ended and stop.

### Post-answer gate (after `NEEDS_INPUT` — "Answer here")

The user has answered the triage questions. Present a dedicated gate:
- Header: "Triage Decision"
- Question: "You've provided the information triage was missing. How would you like to proceed?"
- Options:
  - "Proceed with my answers" — use the answers as context and run the PM Agent
  - "No action — stop pipeline" — end the pipeline; nothing further is generated
  - "Redirect approach" — provide a different direction; I'll run the PM Agent with that

Handle the choice:
- **"Proceed with my answers"** — record the triage report's recommendation (if any)
  combined with the user's answers as `APPROVED_APPROACH` and continue to Phase 1.
- **"No action — stop pipeline"** — inform the user the pipeline has ended with no changes
  and stop. Do not run any further phase.
- **"Redirect approach"** — record the user's direction as `APPROVED_APPROACH` and continue
  to Phase 1.

### Jira comment posting flow (NEEDS_INPUT only)

**Guardrail:** Reads are free; posting a comment is a Jira write — always proposed
first with exact text shown, posted only on explicit approval. Never post internal
triage reasoning or root-cause analysis — only the reporter-facing questions.

1. **Compose the comment.** Build a clean, numbered list of questions from the
   report's "Questions for the reporter" section. Add a trailing attribution line:
   `-- posted by triage assistant`

2. **Show the exact text.** Display the full comment body to the user and ask:
   - Header: "Confirm Jira Comment"
   - Question: "I will post the following comment on <ISSUE_KEY>:\n\n```\n<comment text>\n```\n\nPost this comment?"
   - Options:
     - "Yes, post it" — proceed with the POST
     - "Edit first" — let me revise the text before posting
     - "Cancel" — do not post; return to the Missing Information gate

   If "Edit first": ask the user for their revised text, then show it again for
   confirmation (loop until "Yes, post it" or "Cancel").

3. **Post via curl.** Use the Jira REST API. Build the ADF body as a PowerShell
   hashtable, serialize with `-Compress`, write to a UTF-8 temp file, and pass
   `-d "@file"` to curl (inline `-d` breaks on PowerShell due to quoting):

   ```powershell
   $token = $env:ATLASSIAN_AUTH_BASIC
   $bodyObj = @{
     body = @{
       type = "doc"; version = 1
       content = @(
         @{ type = "paragraph"; content = @(
           @{ type = "text"; text = "Hi," }
         )},
         @{ type = "paragraph"; content = @(
           @{ type = "text"; text = "Could you please clarify:" }
         )},
         @{ type = "orderedList"; attrs = @{ order = 1 }; content = @(
           @{ type = "listItem"; content = @(@{ type = "paragraph"; content = @(
             @{ type = "text"; text = "<question 1>" }
           )})},
           @{ type = "listItem"; content = @(@{ type = "paragraph"; content = @(
             @{ type = "text"; text = "<question 2>" }
           )})}
           # ... one listItem per question
         )},
         @{ type = "paragraph"; content = @(
           @{ type = "text"; text = "-- posted by triage assistant" }
         )}
       )
     }
   }
   $json = $bodyObj | ConvertTo-Json -Depth 10 -Compress
   $tmpFile = "$env:TEMP\jira_comment.json"
   [System.IO.File]::WriteAllText($tmpFile, $json, [System.Text.Encoding]::UTF8)
   & curl.exe -s -X POST `
     -H "Authorization: Basic $token" `
     -H "Content-Type: application/json" `
     -d "@$tmpFile" `
     "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}/comment"
   Remove-Item $tmpFile -ErrorAction SilentlyContinue
   ```

   **Important:** Use `orderedList` with `listItem` nodes for the questions — Jira
   renders these as a numbered list. A single plain `paragraph` with newlines does NOT
   produce a list in the Jira UI.

4. **Handle the response:**
   - **2xx** — success. Inform the user: "Comment posted on <ISSUE_KEY>. Pipeline
     paused — re-run `/sdd <ISSUE_KEY>` when the reporter replies."
   - **401/403** — the configured token appears to be read-only or lacks comment
     permission. Inform the user: "Could not post — token lacks write permission.
     You can post the questions manually, or provide the answers here." Fall back to
     the "Answer here" path.
   - **Other errors** — report the HTTP status and body, offer "Answer here" fallback.

---

## Phase 1 — PM Agent: Jira → Spec

Read the file `.opencode/skills/sdd/phases/pm-agent.md` and pass its full content
as instructions in a `task` prompt:

```
task(subagent_type='general', prompt="""
<contents of phases/pm-agent.md>

Issue: ISSUE_KEY
User context: USER_CONTEXT (or "None" if the user provided no extra text)
Triage report: TRIAGE_PATH
Approved approach: APPROVED_APPROACH
""")
```

**Important:** Pass the triage report path and the human-approved approach. These are
**authoritative** — the PM Agent must bound the spec to the approved approach rather than
the raw ticket, and reuse the triage's root-cause findings instead of re-investigating
from scratch.

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
