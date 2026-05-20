---
name: build-site
description: Orchestrate a complete Liferay site experience from a single prompt. Composes objects, business logic, fragments, pages, roles, and theme into a working site. Use when the user asks to "build a site", "create a site experience", or describes a multi-object, multi-page scenario. Calls all other skills in sequence.
---

# Build Site

One-command orchestrator. The user describes the site; this skill calls the right sub-skills in the right order.

## When to Invoke

- "Build a Bookstore site with an Author object, a Book object, and a home page"
- "Create a site experience for customer onboarding"
- "Scaffold the full job board site"
- Any multi-object, multi-page request that spans data and presentation

## Workflow

The sequence below is the canonical order. Skip phases the user has not requested; do not add phases they have not asked for.

### Phase 0: Scope Confirmation

Before calling any sub-skill, confirm the scope with the user:

1. **Site name** — what to call the site
2. **Objects** — list of entity names with their key fields and relationships
3. **Pages** — list of pages and their purpose
4. **Roles** — named roles and their intended access (viewer, editor, admin)
5. **Theme** — any color, font, or visual requirement (optional)
6. **Mode** — runtime iteration (default: call APIs now) or site initializer capture (generate `siteInitializer` CET for version control)

Proceed only after the user confirms or corrects the scope list.

### Phase 1: Prerequisites

Call `feature-flags` for the full set of flags the workflow needs:

| Scenario | Required Flags |
| --- | --- |
| Site pages via API | `LPD-35443` |
| Fragment composition via API | `LPD-39244` |
| Object entry permissions | `LPD-17564` |
| MCP transport | `LPD-63311` |

Report the gap table. Enable flags only after explicit user confirmation. Bounce Tomcat if any flags are written.

### Phase 2: Transport Selection

Probe for the MCP server:

```bash
curl -sI -H "Accept: text/event-stream" "http://localhost:${PORT}/o/mcp/sse"
```

- **200**: MCP is available. Use the `call-http-endpoint` MCP tool for all subsequent API calls.
- **Not 200**: Fall back to direct `curl` calls with Basic auth.

### Phase 3: Data Model

For each object in the confirmed scope, call `manage-objects`:

1. Create and publish the object definition.
2. Add all fields.
3. Add picklists (if any field references a picklist).
4. Add relationships between objects (parent → child).
5. Add validations.

For each business logic requirement, call `manage-object-logic`:

1. Choose the trigger and action type.
2. Create notification templates if needed.
3. Create the object action.

### Phase 4: Site Creation

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

Save `<site-id>`.

### Phase 5: Fragments

For each unique layout section needed by the page list, call `scaffold-fragment`:

1. Create the fragment source files.
2. Deploy via the fragment importer or inside the site initializer directory.
3. Record the `collectionExternalReferenceCode` and `fragmentEntryKey` for Phase 6.

### Phase 6: Pages

For each page in the confirmed scope, call `manage-pages`:

1. Create the page with the correct type (Content Page default).
2. Compose the page with fragment elements (using keys from Phase 5).
3. Create the navigation menu linking all top-level pages.
4. Set SEO title and description for each page.

### Phase 7: Theme and Design (Optional)

When the user provided visual requirements, call `theme-and-design`:

1. Generate and deploy the `themeCSS` CET.
2. Create and assign the style book.
3. Create the master page with header and footer fragments.

### Phase 8: Roles and Permissions

For each role in the confirmed scope, call `manage-roles-permissions`:

1. Create the role.
2. Assign permissions on each object definition.
3. Assign permissions on each page (restrict visibility if needed).

### Phase 9: Verification

Confirm the site is functional:

```bash
# Site exists
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>" \
  | jq '{id, name}'

# Pages exist
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/site-pages" \
  | jq '[.items[] | {name, friendlyUrlPath, type}]'

# Object definitions published
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions?filter=status%20eq%20%27approved%27" \
  | jq '[.items[] | {name, status}]'

# Probe site home page
curl -sI "http://localhost:${PORT}/web/<site-friendly-url>"
```

Report the final state: objects created, pages created, roles created, site URL.

### Phase 10: Site Initializer Capture (Optional)

When the user wants to commit the site to source control, call `manage-environments` to:

1. Scaffold the `siteInitializer` CET.
2. Export pages, object definitions, and picklists into the CET directory.
3. Copy fragment sources into the CET.
4. Commit and deploy.

## Example Prompt Interpretation

**User**: "Build a Bookstore site with an Author object (name, bio), a Book object (title, isbn, linked to Author), a home page listing Books, and a Reader role that can only view Books."

**Scope confirmation**:
- Objects: Author (name: Text, bio: LongText), Book (title: Text, isbn: Text, authorId: Relationship to Author)
- Pages: Home (Content Page, lists Books via a fragment)
- Roles: Reader (VIEW on Book object, VIEW on Home page)
- Mode: runtime iteration

**Execution order**: Phase 1 (flags) → Phase 2 (MCP probe) → Phase 3 (Author, Book, relationship) → Phase 4 (create site) → Phase 5 (book-list fragment) → Phase 6 (home page + navigation) → Phase 8 (Reader role + permissions) → Phase 9 (verify)

## Handling Partial Failures

When a phase fails:

1. Surface the error and the raw API response to the user.
2. Diagnose the cause (missing flag, validation error, unreachable endpoint).
3. Ask the user whether to retry the failed phase, skip it, or abort.
4. Do not proceed to dependent phases when a prerequisite phase has failed.

## References

- Feature flags: `feature-flags` skill + `rules/feature-flags-catalog.md`
- Objects: `manage-objects` skill
- Business logic: `manage-object-logic` skill
- Fragments: `scaffold-fragment` skill
- Pages: `manage-pages` skill
- Theme: `theme-and-design` skill
- Roles: `manage-roles-permissions` skill
- Site initializer capture: `manage-environments` skill
- Full course list: `rules/learn-topics.md`
