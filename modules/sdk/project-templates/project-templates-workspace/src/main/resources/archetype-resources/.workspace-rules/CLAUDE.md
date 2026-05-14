# Liferay Workspace Rules

This file is the entry point for any AI agent operating inside this workspace. It establishes the runtime context every skill below depends on.

## Establish Context First

Before answering, identify three things:

1. **Workspace root**: the directory containing `gradle.properties` and `settings.gradle`. If neither exists, the user has not initialized a workspace; invoke the `workspace-init` skill.
2. **DXP version**: read `liferay.workspace.product` from `gradle.properties`. Quarterly releases (`-Qx`) and 7.4 lines use the modern path (Client Extensions, Objects, Fragments). Versions below 7.4 use legacy OSGi modules.
3. **Bundle state**: `bundles/` exists when `blade server init` has run. The Tomcat install lives at `bundles/tomcat-*/`. The active HTTP port is the `port` attribute on the `<Connector protocol="HTTP/1.1">` element in `bundles/tomcat-*/conf/server.xml`. Default is 8080.

## Project Paths

| Purpose | Path |
| --- | --- |
| Client extensions | `client-extensions/` |
| OSGi modules | `modules/` |
| Themes | `themes/` |
| Per-environment properties | `configs/{common,local,dev,uat,prod,docker}/` |
| Runtime OSGi configs | `bundles/osgi/configs/` |
| Logs | `bundles/tomcat-*/logs/catalina.out` and `bundles/logs/liferay.<YYYY-MM-DD>.log` |
| Deployed bundles | `bundles/osgi/modules/` and `bundles/osgi/client-extensions/` |

`configs/common/` holds shared settings. `configs/local/` is the default for development. Promotion order is `local` to `dev` to `uat` to `prod`.

## Tooling

Use Blade as the primary CLI. Prefer `blade gw <task>` over invoking Gradle directly; this guarantees the workspace Gradle wrapper. Key commands:

- `blade init` to scaffold a workspace
- `blade server init` to download the bundle
- `blade server start -t` to start Tomcat and tail the log
- `blade gw deploy` to package and deploy a module or client extension
- `blade gw tasks` to list available Gradle tasks

## MCP Server (2025.Q4 and later)

Liferay ships an MCP server behind feature flag `LPD-63311`. When present and enabled, agents prefer the MCP transport over raw `curl`.

| Setting | Value |
| --- | --- |
| URL | `http://localhost:${PORT}/o/mcp/sse` |
| Transport | HTTP Server Sent Events |
| Authorization | `Basic dGVzdEBsaWZlcmF5LmNvbTp0ZXN0` (default `test@liferay.com:test`) |

To detect availability, issue `curl -sI -H 'Accept: text/event-stream' http://localhost:${PORT}/o/mcp/sse`. A 200 response means MCP is reachable. A 404 or 503 means fall back to direct REST calls.

To enable, add `feature.flag.LPD-63311=true` to `configs/local/portal-ext.properties` and bounce Tomcat.

## Skill Index

Skills live under `skills/` and load on demand. Each addresses one workflow.

**Foundations**
- `workspace-init` — bootstrap a workspace and bundle
- `feature-flags` — audit and enable required flags
- `learn-lookup` — consult learn.liferay.com (`rules/learn-topics.md`)
- `deploy-and-verify` — deploy a target and confirm startup

**Backend (data and logic)**
- `manage-objects` — object definitions, fields, relationships, picklists, validations
- `manage-object-logic` — object actions, workflows, notifications
- `setup-oauth` — companion OAuth applications for client extensions
- `integrate-external-data` — back objects with external services

**Frontend (look and composition)**
- `scaffold-fragment` — page fragments with editable regions
- `manage-pages` — site pages, navigation, SEO, page templates
- `theme-and-design` — themes, master pages, style books

**Cross-cutting**
- `scaffold-client-extension` — any of the 26 client extension types
- `manage-roles-permissions` — roles, ACL, object and page permissions
- `manage-environments` — `configs/{env}/`, data migration, siteInitializer capture

**Orchestrator**
- `build-site` — compose objects, pages, fragments, and roles into a complete site experience

## Reference Cards

Reference cards under `rules/` hold the data skills look up. Skills cite the card path explicitly.

- `rules/client-extension-types.md` — 26 client extension types with their yaml and file layout
- `rules/headless-apis.md` — REST modules, base URIs, OAuth scopes
- `rules/feature-flags-catalog.md` — flag table with defaults and dependencies
- `rules/site-initializer-format.md` — site initializer directory tree and batch JSON envelope
- `rules/learn-topics.md` — topic key to learn.liferay.com URL map
- `rules/object-actions-catalog.md` — triggers, conditions, action types
- `rules/oauth-scopes.md` — `Liferay.*` scope strings per Headless module
- `rules/page-types.md` — page types and their applicable APIs

## Information Sources

The authoritative documentation is [learn.liferay.com](https://learn.liferay.com). For URL paths, always start from `rules/learn-topics.md`. When a topic is not mapped, search via `https://learn.liferay.com/search?q=<query>&space=dxp`. The Liferay Portal source code at `https://github.com/liferay/liferay-portal` is useful for architectural patterns and working client extension samples at `workspaces/liferay-sample-workspace/client-extensions/`.
