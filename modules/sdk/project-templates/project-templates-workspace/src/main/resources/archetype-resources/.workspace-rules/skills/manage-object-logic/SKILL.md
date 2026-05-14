---
name: manage-object-logic
description: Add business logic to Liferay Objects via object actions, workflow definitions, and notification templates. Use when the user asks to send a notification on create, trigger an action on update, wire a workflow, or automate any response to object entry lifecycle events. Maps to the Business Logic and Automation section of "Mastering Data Modeling with Liferay Objects".
---

# Manage Object Logic

Attach triggers, conditions, and actions to object definitions. The three extension points are object actions (immediate), Kaleo workflows (multi-step approval), and notification templates (user-facing messages).

## When to Invoke

- "Send a notification when a Book is created"
- "Trigger a webhook on order update"
- "Wire an approval workflow to this object"
- "Run a Groovy script after an entry is deleted"
- Called by `build-site` during the logic phase

## Prerequisites

Object definition must already exist and be published (see `manage-objects`). Client Extension actions require the CET to be deployed first (see `scaffold-client-extension`).

## Workflow

### 1. Choose the Trigger

| Trigger | When It Fires |
| --- | --- |
| `onAfterAdd` | After an entry is created |
| `onAfterUpdate` | After an entry is updated |
| `onAfterDelete` | After an entry is deleted |
| `standalone` | On-demand, invoked explicitly by a user or API call |

### 2. Choose the Action Type

Consult `rules/object-actions-catalog.md` for the full catalog. Summary:

| Action Type | Payload | Notes |
| --- | --- | --- |
| Notification | Template ID | Sends in-app or email notification |
| `addObjectEntry` | Definition name + field map | Creates an entry in another object |
| `updateObjectEntry` | Entry ID + field map | Updates an entry in the same or another object |
| Webhook | URL + secret | HTTP POST to external endpoint |
| Groovy Script | Script body | Only on self-hosted or PaaS; not available on Liferay SaaS |
| Client Extension | CET `objectAction` or `workflowAction` externalReferenceCode | Calls a deployed microservice |

### 3a. Object Action — Notification

Create a notification template first if one does not exist:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/notification/v1.0/notification-templates" \
  -d '{
    "body": {"en_US": "A new [%OBJECT_FIELD_NAME%] was created."},
    "description": "",
    "editorType": "richText",
    "name": "<TemplateName>",
    "objectDefinitionExternalReferenceCode": "<objectERC>",
    "recipientType": "user",
    "subject": {"en_US": "New entry created"},
    "type": "email"
  }'
```

Save the returned `id` as `<template-id>`. Then create the action:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-actions" \
  -d '{
    "active": true,
    "label": {"en_US": "<ActionLabel>"},
    "name": "<actionName>",
    "objectActionExecutorKey": "notification",
    "objectActionTriggerKey": "onAfterAdd",
    "parameters": {
      "notificationTemplateId": <template-id>
    }
  }'
```

### 3b. Object Action — Webhook

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-actions" \
  -d '{
    "active": true,
    "label": {"en_US": "<ActionLabel>"},
    "name": "<actionName>",
    "objectActionExecutorKey": "webhook",
    "objectActionTriggerKey": "onAfterAdd",
    "parameters": {
      "secret": "<hmac-secret>",
      "url": "<https://endpoint.example.com/hook>"
    }
  }'
```

### 3c. Object Action — Client Extension

First deploy the `objectAction` CET via `scaffold-client-extension`. Then reference its `externalReferenceCode`:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-actions" \
  -d '{
    "active": true,
    "label": {"en_US": "<ActionLabel>"},
    "name": "<actionName>",
    "objectActionExecutorKey": "groovy",
    "objectActionTriggerKey": "onAfterAdd",
    "parameters": {
      "clientExtensionEntryExternalReferenceCode": "<cet-erc>"
    }
  }'
```

Replace `"groovy"` with `"objectAction"` when the executor is a CET.

### 4. Kaleo Workflow

Deploy a workflow definition when the object requires multi-step review or approval.

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-workflow/v1.0/workflow-definitions" \
  -d '{
    "active": true,
    "name": "<WorkflowName>",
    "title": {"en_US": "<Workflow Title>"},
    "content": "<escaped XML or JSON workflow definition>"
  }'
```

After creating, associate the workflow with the object definition via the Control Panel (Objects → \<Definition\> → Actions → Workflow) or via the REST endpoint:

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/headless-admin-workflow/v1.0/workflow-definitions/<id>/assign-to-object" \
  -d '{"objectDefinitionId": <definition-id>}'
```

### 5. Verify Object Actions

```bash
curl -s -u "test@liferay.com:learn" \
  "http://localhost:${PORT}/o/object-admin/v1.0/object-definitions/<definition-id>/object-actions" \
  | jq '[.items[] | {name, objectActionTriggerKey, objectActionExecutorKey, active}]'
```

Confirm each action is `"active": true`.

### 6. Test the Trigger

Create a test entry and check the expected side effect (email received, webhook payload, other entry created):

```bash
curl -s -u "test@liferay.com:learn" \
  -H "Content-Type: application/json" \
  -X POST "http://localhost:${PORT}/o/c/<pluralLabel>/" \
  -d '{"<fieldName>": "test value"}'
```

## References

- Object actions catalog: `rules/object-actions-catalog.md`
- Object actions general: `learn-lookup docs.objects`
- Workflow (Kaleo) definitions: consult `learn-lookup` — search "kaleo workflow definition"
