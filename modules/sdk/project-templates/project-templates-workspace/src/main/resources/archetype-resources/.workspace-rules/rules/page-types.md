# Page Types

Reference for the `manage-pages` skill. Covers all page and template types available in Liferay DXP and which APIs handle each.

## Renderable Page Types

These types appear as pages that visitors can navigate to.

| Type | Key | Use Case | API |
| --- | --- | --- | --- |
| Content Page | `content` | Fragment-based layout; the default for new pages | headless-admin-site: `POST /sites/{id}/site-pages` with `"type": "content"` |
| Widget Page | `portlet` | Legacy portlet layout; compatibility with old portlet apps | headless-admin-site: `POST /sites/{id}/site-pages` with `"type": "portlet"` |
| Link to URL | `url` | Redirect node in the navigation hierarchy | headless-admin-site: `POST /sites/{id}/site-pages` with `"type": "url"` |
| Embedded | `embedded` | Iframe pointing to an external URL | headless-admin-site: `POST /sites/{id}/site-pages` with `"type": "embedded"` |
| Full Page Application | `full_page_application` | Single portlet occupying the entire page | headless-admin-site: `POST /sites/{id}/site-pages` with `"type": "full_page_application"` |

## Reusable Template Types

These are blueprints; they are not directly accessible as URLs.

| Type | Purpose | API |
| --- | --- | --- |
| Page Template | Reusable fragment layout for new pages | headless-admin-site: `POST /sites/{id}/page-templates` |
| Master Page Template | Persistent header/footer wrapping all assigned pages | headless-admin-site: `POST /sites/{id}/master-pages` |
| Display Page Template | Per-entry landing page for a content type or object | headless-admin-site: `POST /sites/{id}/display-page-templates` |

## Content Page Structure

A Content Page `pageDefinition` is a tree of `pageElements`. Each element has a `type` and a `definition`.

| Element Type | Description |
| --- | --- |
| `Root` | Top-level container; every page has exactly one |
| `Section` | Layout row, sets columns and spacing |
| `ColumnDefinition` | Column within a section (width, offset) |
| `Fragment` | A deployed fragment identified by `collectionExternalReferenceCode` + `fragmentEntryKey` |
| `Widget` | A portlet embedded in a Content Page |
| `Row` | Grid row inside a section |

Minimal `pageDefinition` for an empty content page:

```json
{
  "pageElement": {
    "pageElements": [],
    "type": "Root"
  },
  "version": "1.0"
}
```

## Display Page Template Binding

The `contentType` field determines which entity type the template renders:

| Entity | `contentType` | `contentSubtype` |
| --- | --- | --- |
| Blog Post | `com.liferay.blogs.model.BlogsEntry` | (empty) |
| Web Content | `com.liferay.journal.model.JournalArticle` | content structure ERC |
| Document | `com.liferay.portal.kernel.repository.model.FileEntry` | (empty) |
| Liferay Object entry | `com.liferay.object.model.ObjectEntry` | object definition ERC |

Only one display page template per `contentType` + `contentSubtype` combination can be marked default. The default template is used when a content item's URL is visited without an explicit template in the path.

## Feature Flags for Page APIs

| Flag | Default | Unlocks |
| --- | --- | --- |
| `LPD-35443` | off | Public layout (page) REST API via headless-admin-site |
| `LPD-38869` | on | Private layout REST access |
| `LPD-39244` | off | Fragment and page composition REST API |

Enable required flags via `feature-flags` skill before calling the page APIs.

## Navigation Menu Item Types

| `type` | What It Links To |
| --- | --- |
| `layout` | A page in the current site (by UUID or friendly URL) |
| `url` | An arbitrary absolute URL |
| `node` | A non-clickable label grouping child items |
| `asset-publisher` | Dynamic list from Asset Publisher portlet |

## References

- Page settings UI reference: `learn-lookup docs.page-settings`
- Content page editor UI: `learn-lookup docs.content-page-editor`
- Pages and navigation course: `learn-lookup course.pages`
- headless-admin-site API: `rules/headless-apis.md`
