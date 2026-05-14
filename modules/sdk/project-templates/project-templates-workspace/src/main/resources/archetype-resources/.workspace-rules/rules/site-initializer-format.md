# Site Initializer Format

Reference for the `manage-environments` and `scaffold-client-extension` skills when capturing or building a `siteInitializer` CET.

A site initializer is a client extension of type `siteInitializer`. When deployed and triggered, it creates a fully configured site from the directory tree below.

## Directory Tree

```
client-extensions/<name>/
  client-extension.yaml
  site-initializer/
    batch/                        # Bulk data import via Headless Batch Engine
      <NN-entity-name>.batch-engine-data.json
    documents/                    # Documents and media
      group/
        <folder-name>/
          <filename>
    fragments/                    # Page fragments
      group/
        <collection-key>/
          <fragment-name>/
            fragment.json
            index.html
            index.css
            index.js
            configuration.json    # optional
    journal-articles/             # Web content (Journal) articles
      <article-name>.xml
      <article-name>.json
    layout-page-templates/        # Page templates and master pages
      display-page-templates/
        <template-name>/
          page-definition.json
          page-template.json
      master-pages/
        <master-name>/
          page-definition.json
          page-template.json
    layout-set/                   # Site-wide navigation and theme settings
      public/
        metadata.json
    layouts/                      # Site pages
      <NN-page-name>/             # NN prefix controls creation order
        page.json                 # Page metadata (type, name, friendlyURL)
        page-definition.json      # Content Page fragment composition
        <NN-child-page>/          # Nested child pages
          page.json
          page-definition.json
    roles.json                    # Site roles
    style-books/                  # Style book entries
      <style-book-name>/
        style-book.json
    thumbnail.png                 # Site thumbnail (displayed in Site Admin)
```

## Batch Engine Data Format

Files under `batch/` are named `<NN-entity-name>.batch-engine-data.json`. The `NN` prefix controls import order (lower numbers first).

```json
{
  "configuration": {
    "className": "com.liferay.headless.admin.list.type.dto.v1_0.ListTypeDefinition",
    "multiCompany": true,
    "parameters": {
      "containsHeaders": "true",
      "createStrategy": "UPSERT",
      "importStrategy": "ON_ERROR_FAIL",
      "updateStrategy": "UPDATE"
    },
    "taskItemDelegateName": "DEFAULT"
  },
  "items": [
    {
      "externalReferenceCode": "<ERC>",
      ...
    }
  ]
}
```

| Field | Purpose |
| --- | --- |
| `configuration.className` | Fully qualified DTO class name; determines which Headless endpoint is called |
| `configuration.multiCompany` | `true` to import on all virtual instances |
| `parameters.createStrategy` | `INSERT` (fail on duplicate) or `UPSERT` (update if exists) |
| `parameters.importStrategy` | `ON_ERROR_FAIL` (halt on first error) or `ON_ERROR_CONTINUE` |
| `items` | Array of entity objects matching the DTO schema |

Common `className` values:

| Entity | `className` |
| --- | --- |
| Picklist | `com.liferay.headless.admin.list.type.dto.v1_0.ListTypeDefinition` |
| Object Definition | `com.liferay.object.admin.rest.dto.v1_0.ObjectDefinition` |
| Object Folder | `com.liferay.object.admin.rest.dto.v1_0.ObjectFolder` |
| Role | `com.liferay.headless.admin.user.dto.v1_0.Role` |
| User | `com.liferay.headless.admin.user.dto.v1_0.UserAccount` |

## `page.json` Format

```json
{
  "friendlyURL": "/home",
  "hidden": false,
  "name": "Home",
  "name_i18n": {
    "en_US": "Home"
  },
  "permissions": [
    {
      "actionIds": ["VIEW"],
      "roleName": "Guest",
      "scope": 4
    }
  ],
  "private": false,
  "system": false,
  "type": "Content"
}
```

`type` values: `"Content"`, `"Portlet"`, `"URL"`, `"Embedded"`.

## `page-definition.json` Format

Mirrors the `pageDefinition` field of the Headless Admin Site page API. Minimum shape for an empty content page:

```json
{
  "pageElement": {
    "pageElements": [],
    "type": "Root"
  },
  "version": "1.0"
}
```

Add fragment elements under `pageElements` to compose the layout.

## `layout-set/public/metadata.json`

Controls navigation menu visibility and theme assignment for the public (non-private) layout set:

```json
{
  "themeId": "classic_WAR_classictheme",
  "colorSchemeId": "01"
}
```

For themeCSS CETs leave `themeId` as `"classic_WAR_classictheme"` and control appearance entirely from the CET.

## `client-extension.yaml` for the Initializer

```yaml
<workspace-id>-site-init:
  name: <WorkspaceId> Site Initializer
  oAuthApplicationHeadlessServerExternalReferenceCode: <workspace-id>-site-oauth
  type: siteInitializer

<workspace-id>-site-oauth:
  name: <WorkspaceId> Site OAuth
  scopes:
    - Liferay.Headless.Admin.Site.everything
    - Liferay.Headless.Admin.Content.everything
    - Liferay.Object.everything
    - Liferay.Headless.Admin.User.everything
    - Liferay.Headless.Batch.Engine.everything
  type: oAuthApplicationHeadlessServer
```

## Triggering the Initializer

After deploying, open Control Panel → Sites → Add and select the site initializer from the template list. The name matches the `name` field in `client-extension.yaml`.

Alternatively, via the REST API:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites" \
  -d '{
    "membershipType": "open",
    "name": "<Site Name>",
    "templateType": "site-initializer",
    "templateExternalReferenceCode": "<workspace-id>-site-init"
  }'
```

## References

- Sample site initializer: `workspaces/liferay-sample-workspace/client-extensions/liferay-sample-site-initializer/`
- Production site initializer: `modules/apps/site-initializer/site-initializer-cms/`
- CET type details: `rules/client-extension-types.md`
- OAuth scopes: `rules/oauth-scopes.md`
