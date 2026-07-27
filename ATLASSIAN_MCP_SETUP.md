# Atlassian MCP Server Setup

The `opencode.json` in this project is configured to use Atlassian's official remote MCP server. This gives AI tools access to Jira and Confluence data.

## Prerequisites

- An Atlassian Cloud account with access to https://servoy-cloud.atlassian.net
- A personal MCP API token (not a regular API token)
- Your organization admin must have enabled API token authentication for the Rovo MCP server

## Step 1: Create an MCP API Token

Go to https://id.atlassian.com/manage-profile/security/api-tokens?autofillToken&expiryDays=max&appId=mcp&selectedScopes=all and create a new token with MCP scopes.

## Step 2: Set the Environment Variable

The `ATLASSIAN_AUTH_BASIC` environment variable must contain the base64-encoded value of `your-email:your-mcp-token`.

### PowerShell (Windows)

```powershell
$env:ATLASSIAN_AUTH_BASIC = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes("your.email@servoy.com:YOUR_MCP_TOKEN"))
```

### Bash (Linux/Mac)

```bash
export ATLASSIAN_AUTH_BASIC=$(echo -n "your.email@servoy.com:YOUR_MCP_TOKEN" | base64)
```

Replace `your.email@servoy.com` with your Atlassian email and `YOUR_MCP_TOKEN` with the token from step 1.

To make this permanent, add the export to your shell profile (e.g. `~/.bashrc`, `~/.zshrc`) or set it as a system environment variable on Windows.

## Step 3: Start opencode

Start opencode from the same terminal where you set the environment variable. The Atlassian MCP server should now be available.

## How it works

The `opencode.json` uses the variable syntax `{env:ATLASSIAN_AUTH_BASIC}` to inject the environment variable into the Authorization header at startup.

## Troubleshooting

- **"Your site admin must authorize this app"** â A site admin must first authorize the MCP app.
- **"slauthtoken: authorization header found, 'slauth' prefix missing"** â You're using a regular API token instead of an MCP token. Create one via the link in Step 1.
- **Authentication fails** â Verify your admin has enabled API token auth in the Rovo MCP server settings (Atlassian Administration > Security > Rovo MCP server).
- **Token expired** â Create a new token and update the environment variable.
