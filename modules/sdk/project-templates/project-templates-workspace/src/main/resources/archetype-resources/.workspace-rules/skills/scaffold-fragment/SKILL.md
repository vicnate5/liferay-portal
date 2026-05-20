---
name: scaffold-fragment
description: Create a Liferay page fragment — an HTML/CSS/JS building block that marketers drop onto Content Pages. Use when the user asks to create a fragment, build a hero section, or make a reusable page component. Maps to the Frontend Developer Learning Path and "Mastering Liferay Pages and Navigation".
---

# Scaffold Fragment

Generate the source files for a Liferay page fragment and deploy it to the running portal so it appears in the Content Page editor fragment palette.

## When to Invoke

- "Create a fragment", "make a hero section", "build a card component"
- "I need a reusable page widget that marketers can configure"
- Called by `build-site` or `manage-pages` when a page composition requires a custom fragment

## Fragment Types

| Type | Use Case | Key Feature |
| --- | --- | --- |
| Component | General-purpose UI block | `data-lfr-editable` regions, configuration fields |
| Section | Full-width layout block | Wraps other fragments or sets background |
| Form | Entry form tied to an object | `data-lfr-form-*` attributes |
| React / Custom Element | Complex interactive widget | Delivered as a custom element CET alongside the fragment |

For complex interactive widgets, use `scaffold-client-extension` with type `customElement` and reference it from the fragment's HTML with a `<custom-element-name>` tag.

## Workflow

### 1. Choose Collection and Name

Fragments belong to a collection. Use an existing collection or create one. The collection name maps to the directory under `client-extensions/<site-initializer>/site-initializer/fragments/group/`.

When delivering fragments inside a `siteInitializer` CET, path:

```
client-extensions/<name>/site-initializer/fragments/group/<collection-name>/<fragment-name>/
```

When delivering fragments as a standalone import, path:

```
client-extensions/<name>/fragments/group/<collection-name>/<fragment-name>/
```

### 2. Generate the Files

Create four files for each fragment:

**`fragment.json`** — metadata

```json
{
  "cssPath": "index.css",
  "htmlPath": "index.html",
  "jsPath": "index.js",
  "name": "<Fragment Display Name>",
  "type": "component"
}
```

**`index.html`** — markup with editable regions

```html
<div class="fragment-<name>">
  <div data-lfr-editable-id="image" data-lfr-editable-type="image">
    <img alt="" src="" />
  </div>
  <div data-lfr-editable-id="title" data-lfr-editable-type="rich-text">
    <h2>Heading</h2>
  </div>
  <div data-lfr-editable-id="body" data-lfr-editable-type="rich-text">
    <p>Body text here.</p>
  </div>
  <a data-lfr-editable-id="link" data-lfr-editable-type="link" href="#">
    Learn more
  </a>
</div>
```

Editable type values: `rich-text`, `text`, `image`, `link`, `html`, `backgroundImage`.

**`index.css`** — scoped styles

```css
.fragment-<name> {
  padding: 2rem;
}

.fragment-<name> h2 {
  font-size: 2rem;
  margin-bottom: 1rem;
}
```

**`index.js`** — optional behavior (empty file if none)

```javascript
/* Fragment JS — runs once per fragment instance on the page */
const fragmentElement = fragmentNamespace.element;
// fragmentElement is the fragment's root DOM element
```

### 3. Add Configuration Fields (Optional)

Create `configuration.json` to expose configurable options in the Content Page editor sidebar:

```json
{
  "fieldSets": [
    {
      "fields": [
        {
          "dataType": "string",
          "defaultValue": "primary",
          "label": "Button Style",
          "name": "buttonStyle",
          "type": "select",
          "typeOptions": {
            "validValues": [
              {"value": "primary"},
              {"value": "secondary"},
              {"value": "link"}
            ]
          }
        }
      ],
      "label": "Styling"
    }
  ]
}
```

Access the value in `index.html` with `[configuration.buttonStyle]` or in `index.js` via `configuration.buttonStyle`.

### 4. Deploy

**Option A — Via site initializer (recommended for persistent fragments):**
The fragment lives inside a `siteInitializer` CET. Run `deploy-and-verify` on the CET root.

**Option B — Via standalone fragment collection CET:**
```bash
cd client-extensions/<fragment-collection-name>
blade gw deploy
```

**Option C — Direct import via REST API (for quick iteration):**

```bash
# Zip the collection directory first
cd client-extensions/<name>/fragments
zip -r fragment-collection.zip group/<collection-name>/

# Import to a specific site
curl -s -u "test@liferay.com:learn" \
  -X POST "http://localhost:${PORT}/o/headless-admin-content/v1.0/sites/<siteId>/fragment-collections/import" \
  -F "file=@fragment-collection.zip"
```

### 5. Verify

Open the Content Page editor at the target site. The fragment collection should appear in the left panel under Fragments. Drag the fragment onto the page and confirm editable regions are highlighted.

Check the browser console for JS errors from `index.js`. Check `bundles/logs/liferay.<date>.log` for import errors.

## Fragment Naming Conventions

- Collection key: `kebab-case`
- Fragment name: `kebab-case`
- CSS class prefix: `fragment-<name>` to avoid global collisions
- Editable ID: `camelCase`, unique within the fragment

## References

- Fragment development intro: `learn-lookup docs.fragments.intro`
- Fragment reference: `learn-lookup docs.fragments`
- Frontend developer path: `learn-lookup path.frontend-developer`
- Pages and navigation course: `learn-lookup course.pages`
