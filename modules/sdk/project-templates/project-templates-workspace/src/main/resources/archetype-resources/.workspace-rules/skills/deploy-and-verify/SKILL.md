---
name: deploy-and-verify
description: Deploy a client extension, module, or theme to the running Liferay server and verify it started. Use at the end of any skill that produces deployable artifacts, when the user asks to deploy, when verifying that a previous deployment took effect, or when troubleshooting a bundle that does not appear in Liferay.
---

# Deploy and Verify

Package a target, push it to the running Tomcat, and confirm OSGi started the bundle.

## When to Invoke

- A build skill (`scaffold-client-extension`, `scaffold-fragment`, `theme-and-design`, etc.) finishes producing source files.
- The user says "deploy", "redeploy", "push to Liferay".
- A previously deployed bundle is missing or in an inconsistent state.

## Workflow

### 1. Identify the Target

Target type drives the command:

| Target type | Detection | Command |
| --- | --- | --- |
| Client extension | `client-extension.yaml` under `client-extensions/<name>/` | `cd client-extensions/<name> && blade gw deploy` |
| OSGi module | `bnd.bnd` and `build.gradle` under `modules/<name>/` | `cd modules/<name> && blade gw deploy` |
| Theme | `liferay-theme.json` under `themes/<name>/` | `cd themes/<name> && blade gw deploy` |
| Fragment collection (file-based) | `fragments/group/<collection>/` under a site initializer | Deploys via the parent `siteInitializer` client extension |

When the target lives across multiple modules (siteInitializer with batch + layouts + fragments), deploy from the client extension root.

### 2. Run the Deploy

Always invoke through Blade: `blade gw deploy`. Blade resolves the Gradle wrapper and copies the resulting archive into `bundles/osgi/client-extensions/` or `bundles/osgi/modules/`.

Capture the build output. A failure here is a build problem; surface the message and stop.

### 3. Tail for STARTED

Watch `bundles/logs/liferay.<YYYY-MM-DD>.log` until either of these markers appears:

- `STARTED <bundle-symbolic-name>_<version>` — success
- `Error processing <path>` or stack trace mentioning the bundle — failure

Use `bash` with `until grep` rather than `tail -F` polling. Example:

```bash
LOG="bundles/logs/liferay.$(date +%Y-%m-%d).log"
until grep -q "STARTED <bsn>" "$LOG"; do sleep 2; done
```

The bundle symbolic name comes from `Bundle-SymbolicName` in `bnd.bnd`, or from `client-extension.yaml` `id` (prefixed by the workspace project ID).

### 4. Smoke Check

After STARTED:

- **Custom element / iframe / globalCSS / globalJS**: hit a page that hosts the resource and verify the network panel loads it.
- **Theme**: visit a site using the theme and confirm the styles applied.
- **Object action / workflow action / notification type / object validation rule / object entry manager**: visit Control Panel and confirm the entry appears in the relevant admin panel (Objects → Actions, Workflow → Definitions, etc.).
- **Batch / site initializer**: verify the configured site or data exists by listing it via REST (e.g. `GET /o/headless-admin-site/v1.0/sites`).

### 5. Troubleshoot

When STARTED never appears:

- Tomcat down: probe `http://localhost:${PORT}/`. If down, start it.
- Bundle in `INSTALLED` state but not `ACTIVE`: open a Gogo shell with `telnet localhost 11311` and run `lb | grep <bsn>` to see the wired state, then `diag <id>` to read the resolution error.
- Stale cache: stop Tomcat, delete `bundles/osgi/state/` and `work/`, start Tomcat.
- Missing dependency: read `bnd.bnd` `Import-Package` against the OSGi registry.

## References

- Blade CLI: `https://learn.liferay.com/w/dxp/development/tooling/blade-cli`
- Client extensions: `https://learn.liferay.com/w/dxp/development/client-extensions`
