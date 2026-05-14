---
name: manage-roles-permissions
description: Create roles, assign permissions on objects and pages, and manage site memberships via Headless Admin User and object collaborators APIs. Use when the user asks to add a role, grant permissions, make a page private, or control who can view or edit an object's entries. Requires feature flag LPD-17564 for per-entry object permissions.
---

# Manage Roles and Permissions

Create roles and assign the minimum required permissions for objects, pages, and sites.

## When to Invoke

- "Create a Reader role", "add a role that can only view Books"
- "Grant edit permissions on this object to Site Members"
- "Make this page visible only to authenticated users"
- "Give the Sales role permission to add and update Orders"
- Called by `build-site` during the roles and ACL phase

## Prerequisites

Feature flag `LPD-17564` is required for the object collaborators API (per-object-definition permissions). Verify via `feature-flags` skill.

## Workflow

### 1. Create a Role

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-user/v1.0/roles" \
  -d '{
    "name": "<RoleName>",
    "roleType": "regular",
    "titleI18n": {"en_US": "<Role Display Name>"}
  }'
```

`roleType` values:

| Value | Scope |
| --- | --- |
| `regular` | Portal-wide; applies across all sites |
| `site` | Site-scoped; membership and permissions are site-specific |
| `organization` | Organization-scoped |

Save the returned `id` as `<role-id>`.

### 2. Assign Users to a Role

```bash
# Assign a user account to a regular role
curl -s -u "test@liferay.com:learn" \
  -X POST "http://localhost:${PORT}/o/headless-admin-user/v1.0/roles/<role-id>/association/user-account/<user-id>"
```

For site roles, use the site membership API instead:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-site/v1.0/sites/<site-id>/site-members" \
  -d '{
    "roleName": "<RoleName>",
    "userId": <user-id>
  }'
```

### 3. Grant Permissions on an Object Definition

Object-level permissions control which roles can create, view, update, or delete entries for that object definition.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X PUT "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/permissions" \
  -d '{
    "permissions": [
      {
        "actionIds": ["VIEW", "PERMISSIONS"],
        "roleId": <role-id>
      }
    ]
  }'
```

Common `actionIds` for object definitions:

| Action ID | Meaning |
| --- | --- |
| `ADD_OBJECT_ENTRY` | Create entries |
| `VIEW` | View the object in site and admin UI |
| `PERMISSIONS` | Manage permissions on this object |

### 4. Grant Permissions on Object Entries (Requires LPD-17564)

Per-entry permissions are managed via the object collaborators API. Ensure `LPD-17564` is enabled before calling.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X PUT "http://localhost:${PORT}/o/c/<pluralLabel>/<entry-id>/permissions" \
  -d '{
    "permissions": [
      {
        "actionIds": ["VIEW", "UPDATE"],
        "roleId": <role-id>
      }
    ]
  }'
```

Common `actionIds` for object entries:

| Action ID | Meaning |
| --- | --- |
| `VIEW` | Read the entry |
| `UPDATE` | Edit the entry |
| `DELETE` | Delete the entry |
| `PERMISSIONS` | Manage permissions on this entry |

### 5. Grant Permissions on a Site Page

Page-level permissions control visibility and edit access.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X PUT "http://localhost:${PORT}/o/headless-admin-site/v1.0/site-pages/<page-id>/permissions" \
  -d '{
    "permissions": [
      {
        "actionIds": ["VIEW"],
        "roleId": <guest-role-id>
      },
      {
        "actionIds": ["VIEW", "UPDATE"],
        "roleId": <role-id>
      }
    ]
  }'
```

To make a page visible only to authenticated users, remove the VIEW permission from the Guest role. To make it fully private, also remove it from the User role and grant VIEW only to the target role.

### 6. Verify Roles and Permissions

```bash
# List roles
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/headless-admin-user/v1.0/roles" \
  | jq '[.items[] | {id, name, roleType}]'

# Check permissions on an object definition
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/permissions" \
  | jq '.'
```

## Permission Design Principles

- Assign the minimum set of `actionIds` that satisfies the workflow requirement.
- Use site roles (not regular roles) for permissions that should vary per site.
- The Guest role represents unauthenticated visitors. Remove VIEW from Guest to require login.
- The User role represents any authenticated user. Remove VIEW from User and grant to a specific role to restrict access.

## References

- Headless Admin User API: `rules/headless-apis.md` (headless-admin-user section)
- Object collaborators flag: `rules/feature-flags-catalog.md` (LPD-17564)
- Object permissions: `learn-lookup docs.objects`
