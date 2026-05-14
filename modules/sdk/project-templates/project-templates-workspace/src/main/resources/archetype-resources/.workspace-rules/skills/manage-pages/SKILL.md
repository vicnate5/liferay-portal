---
name: manage-pages
description: Create and configure site pages, navigation menus, display page templates, page templates, and SEO settings via the Headless Admin Site API. Use when the user asks to create a page, set up navigation, build a display page template for an object, or configure page SEO. Requires feature flag LPD-35443. Maps to "Mastering Liferay Pages and Navigation".
---

# Manage Pages

Create and wire site pages, navigation menus, and page templates via the Headless Admin Site API (`/o/headless-admin-site/v1.0`). When the MCP server is available, prefer MCP tool calls over raw curl.

## When to Invoke

- "Create a page", "add a home page", "set up the site navigation"
- "Build a display page template for Books"
- "Set the page title, description, and URL"
- Called by `build-site` during the page composition phase

## Prerequisites

Feature flag `LPD-35443` must be on for the public layout API. Verify and enable via `feature-flags` skill.

## Page Types

Consult `rules/page-types.md` for the full table. Common types:

| Type | Use | API |
| --- | --- | --- |
| Content Page | Fragment-based layout | headless-admin-site |
| Widget Page | Portlet-based (legacy) | headless-admin-site |
| Display Page Template | Object/content type landing page | headless-admin-site |
| Page Template | Reusable page blueprint | headless-admin-site |

## Workflow

### 1. Ensure the Site Exists

```bash
# List sites
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites" \
  | jq '[.items[] | {id, name, friendlyUrlPath}]'
```

Save the `id` as `<site-id>`. If the target site does not exist, create it:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites" \
  -d '{
    "membershipType": "open",
    "name": "<Site Name>",
    "templateType": "blank"
  }'
```

### 2. Create a Content Page

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/site-pages" \
  -d '{
    "friendlyUrlPath": "/<page-url-slug>",
    "name_i18n": {"en_US": "<Page Name>"},
    "pageDefinition": {
      "pageElement": {
        "pageElements": [],
        "type": "Root"
      },
      "version": "1.0"
    },
    "title_i18n": {"en_US": "<Page Title>"},
    "type": "content"
  }'
```

Save the returned `id` as `<page-id>`.

### 3. Add Fragment Sections to a Content Page

After creating the page, update the `pageDefinition` to embed fragment references. Use the fragment's `fragmentEntryKey` (from the deployed collection) and the collection's `fragmentCollectionKey`:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X PATCH "http://localhost:${PORT}/o/headless-admin-site/v1.0/site-pages/<page-id>" \
  -d '{
    "pageDefinition": {
      "pageElement": {
        "pageElements": [
          {
            "definition": {
              "fragment": {
                "collectionExternalReferenceCode": "<collection-key>",
                "fragmentEntryKey": "<fragment-key>"
              }
            },
            "type": "Fragment"
          }
        ],
        "type": "Root"
      },
      "version": "1.0"
    }
  }'
```

### 4. Create a Display Page Template

Display page templates bind an object or content type to a page layout so each entry has its own URL.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/display-page-templates" \
  -d '{
    "contentSubtype": "",
    "contentType": "com.liferay.object.model.ObjectEntry",
    "contentTypeLabel": {"en_US": "<ObjectName>"},
    "name": "<Template Name>",
    "pageDefinition": {
      "pageElement": {
        "pageElements": [],
        "type": "Root"
      },
      "version": "1.0"
    }
  }'
```

Replace `contentType` and `contentSubtype` with the Liferay class name string for the target object. For Liferay Objects, use `com.liferay.object.model.ObjectEntry` and set `contentSubtype` to the object definition's ERC.

### 5. Create a Navigation Menu

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/site-navigation-menus" \
  -d '{
    "name": "Main Navigation",
    "siteNavigationMenuItems": [
      {
        "name": "<Menu Item Label>",
        "siteNavigationMenuItems": [],
        "type": "layout",
        "typeSettings": "privateLayout=false\nuuid=<page-uuid>\n"
      }
    ]
  }'
```

The `uuid` is the `friendlyUrlPath` slug or the page UUID from the create response.

### 6. Configure SEO Settings

Update page SEO fields after creation:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X PATCH "http://localhost:${PORT}/o/headless-admin-site/v1.0/site-pages/<page-id>" \
  -d '{
    "customMetaTags": [
      {"key": "description", "value": "<meta description>"}
    ],
    "htmlTitle": {"en_US": "<SEO Title>"},
    "seoSettings": {
      "canonicalURL": {"en_US": "<canonical-url>"},
      "description_i18n": {"en_US": "<meta description>"},
      "robots": "index,follow",
      "title_i18n": {"en_US": "<SEO Title>"}
    }
  }'
```

### 7. Verify

```bash
# List pages
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/site-pages" \
  | jq '[.items[] | {id, name, friendlyUrlPath, type}]'

# Probe the page URL
curl -sI "http://localhost:${PORT}/web/<site-friendly-url>/<page-url-slug>"
```

Expect `200 OK` on the page probe.

## References

- Page types reference: `rules/page-types.md`
- Content page editor UI: `learn-lookup docs.content-page-editor`
- Page settings reference: `learn-lookup docs.page-settings`
- Pages and navigation course: `learn-lookup course.pages`
- headless-admin-site API: `rules/headless-apis.md`
