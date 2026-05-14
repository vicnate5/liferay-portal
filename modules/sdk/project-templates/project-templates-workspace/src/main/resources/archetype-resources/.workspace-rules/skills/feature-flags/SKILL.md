---
name: feature-flags
description: Audit, prompt, and enable Liferay feature flags required by a given workflow. Use when a skill needs a flag set (LPD-63311 for MCP, LPD-35443 for Site API, LPD-17564 for object collaborators, etc.), when the user asks to enable a flag, or when an API returns 404 because a flag-gated endpoint is off.
---

# Feature Flags

Detect the current state of feature flags, report gaps against required sets, and prompt before writing.

## When to Invoke

- A skill declares one or more required flags (the caller passes the list).
- The user says "check feature flags", "enable LPD-XXXXX", or "what flags do I need for this".
- An HTTP 404 on a Headless endpoint suggests the flag is off.

## Workflow

### 1. Read Current State

Check both sources, in order:

1. `bundles/portal-ext.properties` for `feature.flag.LPD-XXXXX=true|false` entries.
2. `bundles/osgi/configs/com.liferay.portal.feature.flag*.config` for runtime overrides.

A flag missing from both files takes the default declared in `rules/feature-flags-catalog.md`.

When Tomcat is running and the user is signed in as `test@liferay.com`, also fetch the UI source of truth at `http://localhost:${PORT}/group/control_panel/manage?p_p_id=com_liferay_configuration_admin_web_portlet_InstanceSettingsPortlet&_com_liferay_configuration_admin_web_portlet_InstanceSettingsPortlet_factoryPid=com.liferay.portal.feature.flag.internal.configuration.FeatureFlagsConfiguration` for confirmation.

### 2. Report the Gap

Print a table with: flag, required state, current state, what the flag unlocks, source line. Use `rules/feature-flags-catalog.md` for the metadata. Group rows by status (`OK`, `NEEDS_ENABLE`, `NEEDS_DISABLE`).

### 3. Prompt Before Writing

Never edit `portal-ext.properties` without explicit confirmation. Ask once, list every flag the write will touch, and only proceed on a clear yes. Do not bundle unrelated flags into the same prompt.

### 4. Write and Bounce

Persist changes to `configs/local/portal-ext.properties` (or the active environment from `liferay.workspace.environment`). Append one line per flag:

```properties
feature.flag.LPD-XXXXX=true
```

After write, run a Tomcat bounce: stop with `bundles/tomcat-*/bin/shutdown.sh`, wait for the HTTP probe to fail, start with `bundles/tomcat-*/bin/startup.sh`, wait for the HTTP probe to succeed.

### 5. Verify

Re-read the state and confirm every required flag is in the desired state. Report success or remaining gaps to the caller skill.

## Critical Flags (Quick Reference)

For full table see `rules/feature-flags-catalog.md`.

| Flag | Default | Unlocks |
| --- | --- | --- |
| `LPD-63311` | off | MCP server (`/o/mcp/sse`) |
| `LPD-35443` | off | Headless Admin Site public layout API |
| `LPD-38869` | on | Headless Admin Site private layout API |
| `LPD-39244` | off | Headless Admin Fragment / composition API |
| `LPD-17564` | off | Object collaborators API |
| `LPD-52006` | off | Object entry folders |
| `LPD-32867` | off | Advanced object validations |

## References

- Feature Flags admin doc: `https://learn.liferay.com/w/dxp/security-and-administration/administration/configuring-liferay/feature-flags`
