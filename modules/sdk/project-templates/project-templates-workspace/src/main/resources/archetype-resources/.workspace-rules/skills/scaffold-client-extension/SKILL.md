---
name: scaffold-client-extension
description: Generate the directory structure and client-extension.yaml for any of the 23 Liferay client extension types. Use when the user asks to create a client extension, scaffold a custom element, add an object action microservice, create a site initializer, or build any other CET. After scaffolding, calls deploy-and-verify to confirm startup.
---

# Scaffold Client Extension

Generate a ready-to-deploy client extension under `client-extensions/<name>/`. The CET type drives the file layout and the companion OAuth entry requirement.

## When to Invoke

- "Create a client extension", "scaffold a custom element", "build a React widget"
- "Add an object action microservice"
- "Create a site initializer"
- "I need a global CSS override" or "inject a JS library on every page"

## Workflow

### 1. Identify the CET Type

If the user did not specify a type, ask. Refer to `rules/client-extension-types.md` for the full list of 23 types grouped by classification:

- **Frontend**: `customElement`, `globalCSS`, `globalJS`, `iframe`, `themeCSS`, `themeFavicon`, `themeSpritemap`, `staticContent`, `jsImportMapsEntry`, `editorConfigContributor`, `fdsCellRenderer`, `fdsFilter`, `commerceCheckoutStep`
- **Microservice**: `objectAction`, `objectValidationRule`, `objectEntryManager`, `workflowAction`, `notificationType`, `captcha`, `commercePaymentIntegration`, `commerceShippingEngine`, `commerceTaxEngine`
- **Configuration**: `oAuthApplicationHeadlessServer`, `oAuthApplicationUserAgent`, `instanceSettings`
- **Batch**: `batch`, `siteInitializer`

### 2. Collect Inputs

| Input | How to Obtain |
| --- | --- |
| Extension name | From user; convert to `kebab-case` for the directory |
| Workspace ID | Read from the `id` field at the root of `client-extension.yaml` in the workspace root |
| Microservice port | For microservice types; default `8081`, or next unused port |
| OAuth scopes | Look up in `rules/oauth-scopes.md` for the CET type |

### 3. Create the Directory

```
client-extensions/<extension-name>/
  client-extension.yaml
```

Additional source files depend on type (see templates below).

### 4. Type-Specific Templates

#### `customElement` (React example)

```yaml
<workspace-id>-<name>:
  cssURLs:
    - "css/main.css"
  friendlyURLMapping: <name>
  htmlElementName: <name>-element
  name: <Display Name>
  portletCategoryName: category.remote-apps
  type: customElement
  urls:
    - "js/main.js"
  useESM: "true"
```

Source layout:
```
client-extensions/<name>/
  client-extension.yaml
  package.json
  src/
    index.jsx       # React entry, exports a web component
  build.gradle      # optional; Blade transpiles via Liferay's frontend Gradle tooling
```

Minimal `src/index.jsx`:
```jsx
import React from 'react';
import {createRoot} from 'react-dom/client';

class MyElement extends HTMLElement {
  connectedCallback() {
    const root = createRoot(this);
    root.render(<App />);
  }
}
customElements.define('<name>-element', MyElement);

function App() {
  return <div className="<name>">Hello from <Display Name></div>;
}
```

#### `globalCSS`

```yaml
<workspace-id>-global-css:
  name: <Display Name> Global CSS
  type: globalCSS
  url: "css/main.css"
```

```
client-extensions/<name>/
  client-extension.yaml
  css/
    main.css
```

#### `globalJS`

```yaml
<workspace-id>-global-js:
  name: <Display Name> Global JS
  scriptElementAttributes:
    async: true
  type: globalJS
  url: "js/main.js"
```

#### `objectAction` (Spring Boot example)

```yaml
<workspace-id>-oauth:
  name: <WorkspaceId> OAuth
  scopes:
    - Liferay.Object.everything
  type: oAuthApplicationHeadlessServer

<workspace-id>-<name>:
  baseURL: "http://localhost:8081"
  name: <Display Name> Action
  oAuthApplicationHeadlessServerExternalReferenceCode: <workspace-id>-oauth
  type: objectAction
```

```
client-extensions/<name>/
  client-extension.yaml
  src/main/java/<package>/
    Application.java
    ObjectActionRestController.java
  src/main/resources/
    application.properties
```

`ObjectActionRestController.java` minimal shape:
```java
@RestController
public class ObjectActionRestController {

    @PostMapping("/")
    public ResponseEntity<String> post(@RequestBody Map<String, Object> body) {
        // body contains the object entry fields
        return ResponseEntity.ok("{}");
    }

}
```

`application.properties`:
```properties
server.port=8081
liferay.oauth.application.external.reference.code=<workspace-id>-oauth
```

#### `siteInitializer`

```yaml
<workspace-id>-site-init:
  name: <Site Name> Initializer
  oAuthApplicationHeadlessServerExternalReferenceCode: <workspace-id>-site-oauth
  type: siteInitializer

<workspace-id>-site-oauth:
  name: <Site Name> OAuth
  scopes:
    - Liferay.Headless.Admin.Site.everything
    - Liferay.Headless.Admin.Content.everything
    - Liferay.Object.everything
    - Liferay.Headless.Admin.User.everything
  type: oAuthApplicationHeadlessServer
```

Populate `site-initializer/` per `rules/site-initializer-format.md`.

### 5. Wire OAuth When Required

If `rules/client-extension-types.md` shows "Yes" in the OAuth column, call `setup-oauth` to add the companion `oAuthApplicationHeadlessServer` entry. The OAuth entry must appear in the same `client-extension.yaml` file.

### 6. Deploy and Verify

Call `deploy-and-verify`. For microservice CETs, also start the microservice process on the declared port before the smoke check.

### 7. Troubleshoot

| Symptom | Check |
| --- | --- |
| Bundle not STARTED | Read `bundles/logs/liferay.<date>.log` for the error; run `diag <id>` in Gogo shell |
| Custom element not in Fragments panel | `htmlElementName` must be a valid custom element name (contains a hyphen) |
| 401 from microservice CET | OAuth companion not deployed or scopes insufficient |
| Site initializer not in template list | CET not `ACTIVE`; redeploy and re-check log |

## References

- CET type catalog: `rules/client-extension-types.md`
- OAuth setup: `setup-oauth` skill
- Site initializer format: `rules/site-initializer-format.md`
- Client extension docs: `learn-lookup docs.client-extensions`
- Backend CET course: `learn-lookup course.backend-cet`
