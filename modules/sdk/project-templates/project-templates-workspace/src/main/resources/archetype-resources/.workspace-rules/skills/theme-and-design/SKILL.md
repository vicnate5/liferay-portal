---
name: theme-and-design
description: Customize the visual appearance of a Liferay site using themeCSS client extensions, master pages, and style books. Use when the user asks to change colors or fonts, create a theme, set up a style book, define a master page header/footer, or apply WCAG accessibility guidelines. Maps to "Mastering Liferay Design Elements".
---

# Theme and Design

Three layers control a site's look and feel. Apply them in this order: themeCSS (base variables and overrides), style book (token values per site), master page (header/footer layout).

## When to Invoke

- "Change the site colors", "apply a brand theme", "create a custom theme"
- "Set up a style book with our design tokens"
- "Define a header and footer for all pages"
- "Create a dark variant of the site"
- Called by `build-site` when the user specifies visual design requirements

## Layer 1: themeCSS Client Extension

A `themeCSS` CET injects custom CSS that overrides Clay Design System variables. This replaces the legacy Liferay theme WAR.

### Scaffold

```
client-extensions/<name>/
  client-extension.yaml
  src/
    css/
      _custom.scss     # Clay variable overrides
      main.scss        # Entry point
```

**`client-extension.yaml`:**

```yaml
<workspace-id>-theme-css:
  clayVersion: "3"
  mainUrl: "css/main.css"
  name: <WorkspaceId> Theme CSS
  type: themeCSS
```

**`src/css/_custom.scss`** — Clay variable overrides:

```scss
// Brand colors
$primary: #0B5FFF;
$secondary: #6B7280;
$success: #287D3C;
$danger: #DA1414;

// Typography
$font-size-base: 1rem;
$font-family-base: "Inter", sans-serif;
$headings-font-weight: 700;

// Border radius
$border-radius: 0.5rem;
$border-radius-lg: 1rem;

// Shadows
$box-shadow: 0 1px 3px rgba(0, 0, 0, 0.12);
```

**`src/css/main.scss`:**

```scss
@import "custom";
```

Build the SCSS to CSS: `blade gw buildClientExtension` or configure the Sass build in `build.gradle`. Then run `deploy-and-verify`.

### Apply to Site

After deployment, go to Site Administration → Design → Theme → Configure and select the deployed theme CSS client extension.

## Layer 2: Style Book

A style book maps Clay token names to site-specific values. It overrides the themeCSS tokens without touching the code.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-content/v1.0/sites/<site-id>/style-books" \
  -d '{
    "name": "<Style Book Name>",
    "styleBookEntryId": 0,
    "tokenValues": {
      "bodyBg": "#FFFFFF",
      "primaryColor": "#0B5FFF",
      "borderRadius": "0.5rem"
    }
  }'
```

Save the returned `id`. Apply the style book to the site via Site Administration → Design → Style Book → select.

Consult `learn-lookup docs.style-books` for the full token name reference.

## Layer 3: Master Page

Master pages define the persistent header and footer that surround all Content Pages assigned to that master.

### Create via API

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/master-pages" \
  -d '{
    "name": "<Master Page Name>",
    "pageDefinition": {
      "pageElement": {
        "pageElements": [
          {
            "definition": {
              "fragment": {
                "collectionExternalReferenceCode": "<header-collection-key>",
                "fragmentEntryKey": "<header-fragment-key>"
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

The Drop Zone fragment should also be included so page-specific fragments can be placed. Reference `fragmentEntryKey: "LAYOUT_DROP_ZONE"` with `collectionExternalReferenceCode: "BASIC_COMPONENT"`.

Consult `learn-lookup docs.master-pages` for the full master page template reference.

## Accessibility

Follow WCAG 2.1 AA as the baseline:

- Color contrast: minimum 4.5:1 for normal text, 3:1 for large text (18pt+). Use a contrast checker before finalizing the `$primary` color.
- Focus indicators: ensure Clay's default focus ring is not overridden to `outline: none` without a replacement.
- Skip link: include a `<a href="#main-content" class="skip-link">Skip to main content</a>` in the master page header.
- Image alt text: enforce via `data-lfr-editable-type="image"` regions, which prompt editors to provide alt text.

## Favicon and Spritemap

To replace the Liferay favicon or icon spritemap, use the companion CET types:

```yaml
<workspace-id>-favicon:
  url: "images/favicon.ico"
  type: themeFavicon

<workspace-id>-spritemap:
  url: "images/icons.svg"
  type: themeSpritemap
```

Deploy alongside the `themeCSS` CET.

## Verify

After deploying and assigning:

1. Open the site home page in the browser.
2. Inspect the `<head>` for `<link>` tags referencing the `main.css` from the themeCSS CET.
3. Confirm brand colors appear in primary buttons and headings.
4. Run a browser contrast audit (DevTools → Accessibility) to validate WCAG compliance.

## References

- Style books: `learn-lookup docs.style-books`
- Master pages: `learn-lookup docs.master-pages`
- Themes: `learn-lookup docs.themes`
- Design elements course: `learn-lookup course.design`
- Client extension types: `rules/client-extension-types.md`
