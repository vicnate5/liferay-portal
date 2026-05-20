---
name: workspace-init
description: Bootstrap a Liferay Workspace and download the Tomcat bundle. Use when the user is starting fresh, when gradle.properties or bundles/ is missing, or when the user asks to set up, initialize, or scaffold a workspace.
---

# Workspace Init

Stand up a working Liferay Workspace from zero. Covers `blade init`, bundle download, environment configs, and first server start.

## When to Invoke

- The user says "set up a workspace", "init liferay workspace", "blade init", or "first time setup".
- `gradle.properties` or `settings.gradle` is missing.
- `bundles/` is missing or empty.
- Tomcat fails to start because the bundle never downloaded.

## Workflow

### 1. Workspace Presence

Check for `gradle.properties` and `settings.gradle` at the working directory.

When both exist, the workspace is initialized. Skip to step 3.

When either is missing, run `blade init -v <version> <name>` from the parent directory and `cd` into the new workspace. Ask the user which DXP version when not specified. Recent quarterly tags include `2025.q4.0`, `2026.q1.0`. The default for a fresh start is the latest quarterly release.

### 2. Bundle Download

Run `blade server init` from the workspace root. This reads `liferay.workspace.bundle.url` from `gradle.properties` and unpacks the Tomcat bundle into `bundles/`.

Confirm `bundles/tomcat-*/` exists before proceeding.

### 3. Environment Configs

The workspace ships five environment folders under `configs/`: `common`, `local`, `dev`, `uat`, `prod`. The active environment is set by `liferay.workspace.environment` in `gradle.properties`. Default is `local`.

`configs/common/portal-setup-wizard.properties` holds settings that apply to all environments. `configs/<env>/portal-ext.properties` holds environment-specific overrides. License files live at `configs/<env>/deploy/`.

### 4. Server Start

Use Blade. Pick the right variant for the user's intent:

- `blade server start -t` to start and tail the log
- `blade server run` to run in the foreground; closing the terminal stops the server
- `blade server start -d` to start in debug mode on port 8000

Wait for the line `Server startup in [N] ms` in `bundles/tomcat-*/logs/catalina.out`. Then probe `http://localhost:${PORT}/`. The default port is 8080.

Default sign-in: `test@liferay.com` / `test`.

### 5. Optional: Enable MCP

If the user intends to drive site building via prompts, hand off to `feature-flags` to enable `LPD-63311` (Liferay MCP server). Requires 2025.Q4 or later.

## Verification

- `gradle.properties` and `settings.gradle` present
- `bundles/tomcat-*/` present
- HTTP request to `http://localhost:${PORT}/` returns 200
- User can sign in with the default credentials

## References

- Workspace docs: `https://learn.liferay.com/w/dxp/development/tooling/liferay-workspace`
- Creating a workspace: `https://learn.liferay.com/w/dxp/development/tooling/liferay-workspace/creating-a-liferay-workspace`
- Blade CLI: `https://learn.liferay.com/w/dxp/development/tooling/blade-cli`
- Course: `https://learn.liferay.com/course/mastering-liferay-workspaces-and-tooling/w-t-introduction/w-t-introduction`
