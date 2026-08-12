# Jira API Reference

Load this file when asked to create, update, read, or link Jira issues.

## Connection

Base URL: `https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3`
Auth: Basic auth via environment variable `ATLASSIAN_AUTH_BASIC` (base64-encoded `email:api-token`).

## Platform detection

Choose the correct shell based on the OS:
- **Windows**: Use PowerShell with `Invoke-RestMethod`
- **macOS/Linux**: Use bash with `curl`

**CRITICAL**: Do NOT use `curl` in PowerShell — it's an alias for `Invoke-WebRequest` and `-s`/`-H` flags will fail with cryptic errors.

---

## Reading an issue

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$response = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}?fields=summary,description,comment,attachment,issuelinks,subtasks,status,priority,components,fixVersions,labels" -Headers $headers
$response | ConvertTo-Json -Depth 20
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue/{ISSUE_KEY}?fields=summary,description,comment,attachment,issuelinks,subtasks,status,priority,components,fixVersions,labels"
```

---

## Downloading an attachment

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/attachment/content/{ATTACHMENT_ID}" -Headers $headers
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -L -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/attachment/content/{ATTACHMENT_ID}"
```

---

## Searching issues (JQL)

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token" }
$response = Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/search?jql={URL_ENCODED_JQL}&fields=summary,status" -Headers $headers
$response | ConvertTo-Json -Depth 20
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -H "Authorization: Basic $TOKEN" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/search?jql={URL_ENCODED_JQL}&fields=summary,status"
```

---

## Creating an issue

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
$jsonBody = '<json string>'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) `
  -ContentType "application/json"
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
JSON_BODY='<json string>'
curl -s -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$JSON_BODY" \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issue"
```

### Available issue types for SVY project

| Name | ID | Use for |
|------|----|---------|
| Task | 10002 | Refactoring, technical work, improvements |
| Bug | 10004 | Defects, problems |
| New Feature | 10045 | New product features |

There is **no** "Improvement" issue type — use "Task" for refactoring/improvements.

### ADF description template

Description must use ADF (Atlassian Document Format) — not plain text or markdown.

```json
{
  "type": "doc",
  "version": 1,
  "content": [
    {
      "type": "paragraph",
      "content": [{"type": "text", "text": "Your paragraph text here"}]
    }
  ]
}
```

Valid block nodes: `paragraph`, `heading` (with `attrs.level`), `bulletList`, `orderedList`, `codeBlock`, `blockquote`, `rule`.
Headings use: `{"type": "heading", "attrs": {"level": 2}, "content": [{"type": "text", "text": "..."}]}`
List items: `{"type": "bulletList", "content": [{"type": "listItem", "content": [{"type": "paragraph", "content": [...]}]}]}`

---

## Linking issues

### PowerShell (Windows)

```powershell
$token = $env:ATLASSIAN_AUTH_BASIC
$headers = @{ "Authorization" = "Basic $token"; "Content-Type" = "application/json" }
$jsonBody = '{"type":{"name":"Relates"},"inwardIssue":{"key":"SVY-XXXXX"},"outwardIssue":{"key":"SVY-YYYYY"}}'
Invoke-RestMethod -Uri "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issueLink" `
  -Method POST -Headers $headers `
  -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) `
  -ContentType "application/json"
```

### bash (macOS/Linux)

```bash
TOKEN="$ATLASSIAN_AUTH_BASIC"
curl -s -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"type":{"name":"Relates"},"inwardIssue":{"key":"SVY-XXXXX"},"outwardIssue":{"key":"SVY-YYYYY"}}' \
  "https://api.atlassian.com/ex/jira/7c2b3b79-12a3-4f2c-81e2-0d61b19464b3/rest/api/3/issueLink"
```

Link type names: `"Relates"`, `"Blocks"`, `"Cloners"`, `"Duplicate"`.

---

## Error handling

### PowerShell (Windows)

```powershell
try {
    $response = Invoke-RestMethod -Uri $uri -Method POST -Headers $headers -Body ([System.Text.Encoding]::UTF8.GetBytes($jsonBody)) -ContentType "application/json"
    $response | ConvertTo-Json
} catch {
    $_.Exception.Response.GetResponseStream() | ForEach-Object {
        $reader = New-Object System.IO.StreamReader($_)
        $reader.ReadToEnd()
    }
}
```

### bash (macOS/Linux)

```bash
response=$(curl -s -w "\n%{http_code}" -X POST \
  -H "Authorization: Basic $TOKEN" \
  -H "Content-Type: application/json" \
  -d "$JSON_BODY" \
  "$URL")
http_code=$(echo "$response" | tail -1)
body=$(echo "$response" | sed '$d')
if [ "$http_code" -ge 400 ]; then echo "Error $http_code: $body"; fi
```

---

## Common mistakes to avoid

- Do NOT use `curl` with `-H` flags in PowerShell — use `Invoke-RestMethod` with `-Headers` hashtable
- Do NOT use `{"name": "Improvement"}` as issue type — it doesn't exist, use `"Task"`
- Do NOT pass `-Body $jsonBody` as a plain string in PowerShell — always wrap with `[System.Text.Encoding]::UTF8.GetBytes()`
- Do NOT construct JSON via `ConvertTo-Json` on deeply nested hashtables for the request body — build the JSON string directly to avoid escaping/depth issues
- Do NOT forget to link related issues after creation
- Query issue types first if unsure: `GET /rest/api/3/issue/createmeta/SVY/issuetypes`
