# OAuth Scopes

`Liferay.*` scope strings for Liferay Headless modules. Use the minimum set that covers the API calls a client extension makes.

Each scope grants access to all operations in the module. There are no per-endpoint granular scopes in the standard Liferay OAuth implementation.

## Scope Table

| Module | Scope String | Grants Access To |
| --- | --- | --- |
| headless-admin-site | `Liferay.Headless.Admin.Site.everything` | Site pages, navigation menus, display page templates, master pages |
| headless-admin-content | `Liferay.Headless.Admin.Content.everything` | Structured contents, style books, fragment collections, web content |
| headless-delivery | `Liferay.Headless.Delivery.everything` | Blog posts, documents, structured content (delivery / non-admin) |
| object-admin | `Liferay.Object.everything` | Object definitions, fields, relationships, actions, validations, object entries via `/o/c/` |
| headless-admin-list-type | `Liferay.Headless.Admin.List.Type.everything` | Picklist (list type) definitions and entries |
| headless-admin-user | `Liferay.Headless.Admin.User.everything` | Accounts, users, roles, organizations |
| headless-admin-workflow | `Liferay.Headless.Admin.Workflow.everything` | Workflow definitions, instances, tasks |
| batch-engine | `Liferay.Headless.Batch.Engine.everything` | Batch data import and export task execution |

## Scope Selection by CET Type

| CET Type | Minimum Scopes |
| --- | --- |
| `objectAction` | `Liferay.Object.everything` |
| `workflowAction` | `Liferay.Headless.Admin.Workflow.everything`, `Liferay.Object.everything` |
| `notificationType` | `Liferay.Object.everything` |
| `batchEngineDataImportTaskExecutor` | `Liferay.Headless.Batch.Engine.everything`, `Liferay.Object.everything` |
| `siteInitializer` | `Liferay.Headless.Admin.Site.everything`, `Liferay.Headless.Admin.Content.everything`, `Liferay.Object.everything`, `Liferay.Headless.Admin.User.everything` |
| `objectEntryManager` | `Liferay.Object.everything` |
| Commerce CETs | `Liferay.Headless.Commerce.Admin.everything` (separate module) |

## How Scopes Appear in `client-extension.yaml`

```yaml
<workspace-id>-oauth:
  name: <WorkspaceId> OAuth Application
  scopes:
    - Liferay.Headless.Admin.Site.everything
    - Liferay.Object.everything
  type: oAuthApplicationHeadlessServer
```

Each scope string is one list entry. Liferay validates the list on deploy; unknown scope strings cause deployment to fail with a configuration error.

## Verifying Scope Coverage

If an API call returns 403 (Forbidden), the token's scopes do not cover the endpoint:

1. Check the exact endpoint against the module table above.
2. Add the missing scope to the `oAuthApplicationHeadlessServer` entry in `client-extension.yaml`.
3. Redeploy via `deploy-and-verify`.

A 401 (Unauthorized) means the token itself is not valid — check the OAuth application registration and credentials, not the scopes.

## References

- OAuth 2 application management: Control Panel → OAuth 2 Administration
- `setup-oauth` skill: companion OAuth application generation
- CET type requirements: `rules/client-extension-types.md`
