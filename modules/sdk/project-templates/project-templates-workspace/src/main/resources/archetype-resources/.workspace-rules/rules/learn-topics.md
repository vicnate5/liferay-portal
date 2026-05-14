# Learn Topics Map

Topic key to verified `learn.liferay.com` URL. Every entry was probed against the live site. The `learn-lookup` skill consults this file first; falls back to search when no key matches.

learn.liferay.com restructures category paths without notice. Verify URLs that 404 or 3xx, then update this file rather than papering over with a redirect.

## Courses

Course landing pages 404 at the bare slug. Entries point to the verified introduction lesson, which is the canonical course start page.

| Key | URL |
| --- | --- |
| `course.workspace` | `https://learn.liferay.com/course/mastering-liferay-workspaces-and-tooling/w-t-introduction/w-t-introduction` |
| `course.objects` | `https://learn.liferay.com/course/mastering-data-modeling-with-liferay-objects/ob-introduction/ob-introduction` |
| `course.backend-cet` | `https://learn.liferay.com/course/mastering-liferay-backend-client-extensions/mbcx-introduction/mbcx-introduction` |
| `course.pages` | `https://learn.liferay.com/course/mastering-liferay-pages-and-navigation/pn-introduction/pn-introduction` |
| `course.design` | `https://learn.liferay.com/course/introduction-1` |
| `course.storefronts` | `https://learn.liferay.com/course/mastering-storefronts-with-liferay/msl-introduction/msl-introduction` |

## Learning Paths

| Key | URL |
| --- | --- |
| `path.frontend-developer` | `https://learn.liferay.com/learning-path/frontend-developer` |
| `path.app-developer` | `https://learn.liferay.com/learning-path/liferay-application-developer` |

Note: the application developer page renders course listings via JavaScript. Agents should follow on-page links to individual courses rather than parsing the initial HTML.

## Client Extensions

| Key | URL |
| --- | --- |
| `docs.client-extensions` | `https://learn.liferay.com/w/dxp/development/client-extensions` |
| `docs.cet.object-action-yaml` | `https://learn.liferay.com/w/dxp/development/integrating-microservices/object-action-yaml-configuration-reference` |
| `docs.cet.workflow-action-yaml` | `https://learn.liferay.com/w/dxp/development/integrating-microservices/workflow-action-yaml-configuration-reference` |
| `docs.cet.object-validation-rule-yaml` | `https://learn.liferay.com/w/dxp/development/integrating-microservices/object-validation-rule-yaml-configuration-reference` |

## Objects (Low Code)

| Key | URL |
| --- | --- |
| `docs.objects` | `https://learn.liferay.com/w/dxp/low-code/objects` |
| `docs.objects.relationships` | `https://learn.liferay.com/w/dxp/low-code/objects/creating-and-managing-objects/relationships` |
| `docs.objects.validations` | `https://learn.liferay.com/w/dxp/low-code/objects/creating-and-managing-objects/validations/expression-builder-validations-reference` |
| `docs.objects.picklists` | `https://learn.liferay.com/w/dxp/low-code/objects/picklists` |
| `docs.objects.headless` | `https://learn.liferay.com/w/dxp/low-code/objects/creating-and-managing-objects/managing-objects-with-headless-apis` |

## Site Building (Pages, Fragments, Master Pages, Style Books)

| Key | URL |
| --- | --- |
| `docs.fragments` | `https://learn.liferay.com/w/dxp/site-building/developer-guide/developing-page-fragments` |
| `docs.fragments.intro` | `https://learn.liferay.com/w/dxp/site-building/developer-guide/developing-page-fragments/developing-fragments-intro` |
| `docs.master-pages` | `https://learn.liferay.com/w/dxp/sites/creating-pages/defining-headers-and-footers/managing-master-page-templates` |
| `docs.style-books` | `https://learn.liferay.com/w/dxp/sites/site-appearance/style-books/using-a-style-book-to-standardize-site-appearance` |
| `docs.page-settings` | `https://learn.liferay.com/w/dxp/sites/creating-pages/page-settings/page-settings-ui-reference` |
| `docs.content-page-editor` | `https://learn.liferay.com/en/w/dxp/sites/creating-pages/using-content-pages/content-page-editor-ui-reference` |

## Themes and Look-and-Feel

| Key | URL |
| --- | --- |
| `docs.themes` | `https://learn.liferay.com/w/dxp/development/customizing-liferays-look-and-feel/themes` |

## Tooling and Workspace

| Key | URL |
| --- | --- |
| `docs.workspace` | `https://learn.liferay.com/w/dxp/development/tooling/liferay-workspace` |
| `docs.workspace.create` | `https://learn.liferay.com/w/dxp/development/tooling/liferay-workspace/creating-a-liferay-workspace` |
| `docs.blade` | `https://learn.liferay.com/w/dxp/development/tooling/blade-cli` |

## Integration and Headless APIs

| Key | URL |
| --- | --- |
| `docs.integration` | `https://learn.liferay.com/w/dxp/integration` |
| `docs.headless` | `https://learn.liferay.com/w/dxp/integration/headless-apis` |
| `docs.headless.platform` | `https://learn.liferay.com/w/dxp/integration/headless-apis/using-liferay-as-a-headless-platform` |

## Administration

| Key | URL |
| --- | --- |
| `docs.feature-flags` | `https://learn.liferay.com/w/dxp/security-and-administration/administration/configuring-liferay/feature-flags` |

## AI and MCP

| Key | URL |
| --- | --- |
| `docs.mcp` | `https://learn.liferay.com/w/dxp/ai/using-liferay-as-an-mcp-server` |

## Adding a New Topic

1. Probe the candidate URL with `curl -sI <url>`. Require a 200 response with no redirect.
2. Pick a key in the form `docs.<area>.<topic>` or `course.<name>`.
3. Add the row under the appropriate section.
4. When a URL 404s after addition, fix the entry rather than removing the row; downstream skills cite by key, not URL.
