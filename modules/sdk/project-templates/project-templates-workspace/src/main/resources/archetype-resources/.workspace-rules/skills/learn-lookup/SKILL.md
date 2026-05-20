---
name: learn-lookup
description: Consult learn.liferay.com for authoritative documentation on a Liferay feature. Use whenever a skill needs canonical reference content, when the user asks "how does X work in Liferay", or when an answer must cite a learn.liferay.com URL. The skill resolves a topic key to a verified URL via rules/learn-topics.md, fetches the page, and returns the cited excerpt.
---

# Learn Lookup

Cross-cutting skill. Other skills call this when they need a citation. Users call it directly when they want to read the docs.

## When to Invoke

- Another skill needs to cite a canonical learn.liferay.com page.
- The user asks "how does X work" about a Liferay feature.
- The agent is about to recommend an API or pattern and wants to confirm syntax against the docs.

## Workflow

### 1. Resolve the Topic

Open `rules/learn-topics.md`. Match the user's request to a topic key (e.g. `docs.objects.validations`, `course.objects`, `docs.mcp`). Keys are organized by area: courses, client extensions, objects, site building, themes, tooling, integration, administration, AI/MCP.

When the request is broader than any single key, list 2 or 3 candidate URLs and pick the most specific.

### 2. Verify the URL

`rules/learn-topics.md` carries only verified URLs. If the user supplied a URL not in the map, run `curl -sI <url>` first; treat 404 or 3xx as a stale path and search instead.

### 3. Fetch and Extract

WebFetch the URL. Extract the section that answers the user's question. Cite the URL alongside the excerpt. Do not paraphrase syntax-bearing content (yaml fragments, REST paths, scope strings); quote it verbatim.

### 4. Search Fallback

When no key matches, use `https://learn.liferay.com/search?q=<encoded-query>&space=dxp`. Return the top relevant result with its URL. After a successful search, propose a new key to add to `rules/learn-topics.md` so the next agent finds it directly.

## URL Hygiene

learn.liferay.com restructures category paths without notice. Two known migrations:

- `building-applications/objects/` moved to `low-code/objects/`
- `headless-delivery` moved to `integration/headless-apis`

Treat any 301 or 404 response as a signal that the map needs an update. Update the entry, do not paper over with a redirect.

## References

- All entries: `rules/learn-topics.md`
