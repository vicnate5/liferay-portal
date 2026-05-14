---
name: manage-objects
description: Create, update, and publish Liferay Object definitions — fields, relationships, picklists, and validations. Use when the user asks to create an object, add a field, define a picklist, add a relationship, or set up an object validation. Maps to "Mastering Data Modeling with Liferay Objects".
---

# Manage Objects

CRUD for Liferay Object definitions and their child resources via the Headless Admin Object APIs.

## When to Invoke

- "Create an object", "define a data model", "make a custom entity"
- "Add a field", "add a relationship", "add a picklist"
- "Set up a validation", "publish the object"
- Called by `build-site` during the data model phase

## Prerequisites

Probe the following flags via `feature-flags` before any API call. Record the result for the session — do not re-probe on every call.

| Flag | Default | Required For |
| --- | --- | --- |
| `LPD-17564` | off | Object collaborators API (per-entry permissions) |
| `LPD-52006` | off | Object entry folders (requires `LPD-17564` + `LPD-34594`) |
| `LPD-32867` | off | Advanced (scripted) validations |

Skip flags the user's workflow does not need. Do not enable flags without explicit user confirmation.

## Workflow

### 1. Collect Object Definition Inputs

Gather from the user or infer from context:

- `name` — singular CamelCase label (e.g. `Book`)
- `label` — human-readable singular (e.g. `Book`)
- `pluralLabel` — REST-path-safe plural (e.g. `books`)
- `scope` — `company` (default, global) or `site`
- `storageType` — `default` (Liferay DB) or `salesforce` or `ext-Service` (see `integrate-external-data`)
- Fields list — each with `businessType`, `name`, `label`, `required`

### 2. Create the Object Definition

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions" \
  -d '{
    "label": {"en_US": "<Label>"},
    "name": "<Name>",
    "pluralLabel": {"en_US": "<PluralLabel>"},
    "scope": "company",
    "storageType": "default"
  }'
```

Save the returned `id` as `<definition-id>`.

### 3. Add Fields

For each field in the user's list:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-fields" \
  -d '{
    "businessType": "<businessType>",
    "label": {"en_US": "<FieldLabel>"},
    "name": "<fieldName>",
    "required": false
  }'
```

Common `businessType` values: `Text`, `LongText`, `Integer`, `Decimal`, `Boolean`, `Date`, `DateTime`, `Attachment`, `Relationship`, `Picklist`.

### 4. Add Picklists (When Needed)

Create the picklist first, then reference it in the field:

```bash
# Create list type definition
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-list-type/v1.0/list-type-definitions" \
  -d '{
    "name": "<PicklistName>",
    "listTypeEntries": [
      {"key": "value1", "name": "Value One", "type": ""},
      {"key": "value2", "name": "Value Two", "type": ""}
    ]
  }'
```

Save the returned `id` as `<list-type-id>`. Then add a `Picklist` field referencing `"listTypeDefinitionId": <list-type-id>`.

### 5. Add Relationships

Relationships are defined on the parent object. The `objectDefinitionId2` is the child definition's ID.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<parent-definition-id>/object-relationships" \
  -d '{
    "label": {"en_US": "<RelationshipLabel>"},
    "name": "<relationshipName>",
    "objectDefinitionId2": <child-definition-id>,
    "type": "oneToMany"
  }'
```

Relationship `type` values: `oneToMany`, `manyToMany`, `oneToOne`.

### 6. Add Validations

Requires `LPD-32867` for script-based validations. Expression-builder validations work without any flag.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-validations" \
  -d '{
    "active": true,
    "engine": "function",
    "errorLabel": {"en_US": "<Error message>"},
    "name": "<validationName>",
    "script": "<expression>"
  }'
```

Consult `learn-lookup docs.objects.validations` for expression-builder syntax.

### 7. Publish the Object Definition

An unpublished object has no REST endpoint and no UI entry. Always publish after adding fields and relationships.

```bash
curl -s -u "test@liferay.com:learn" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/publish"
```

After publishing, object entries are available at `/o/c/<pluralLabel>/`.

### 8. Verify

```bash
# List all published definitions
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions?filter=status%20eq%20%27approved%27" \
  | jq '[.items[] | {id, name, status}]'
```

Confirm the definition name appears and `status` is `approved`.

## Object Entry CRUD (After Publishing)

```bash
# Create entry
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/c/<pluralLabel>/" \
  -d '{"<fieldName>": "<value>"}'

# List entries
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/c/<pluralLabel>/"
```

## References

- Object data modeling: `learn-lookup docs.objects`
- Relationships: `learn-lookup docs.objects.relationships`
- Validations: `learn-lookup docs.objects.validations`
- Picklists: `learn-lookup docs.objects.picklists`
- Headless object management: `learn-lookup docs.objects.headless`
- Full API catalog: `rules/headless-apis.md`
