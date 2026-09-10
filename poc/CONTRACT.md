# AI Hub Workspace POC — Integration Contract

This file is the seam between three independently built tracks. Do not change any
signature here without updating this file first.

## Topology

```
Browser
  └── DXP :8080  page hosting the chat custom element (Track C)
        └── OAuth2 token from DXP
              └── Broker :8091  Spring Boot (Track B)
                    └── Sidecar :8090  in Docker (Track A)
                          └── /workspace  Liferay Workspace
                                └── gradlew deploy → /liferay/deploy
                                      └── bind mount → host bundles/deploy
                                            └── DXP hot deploy
```

Ports are fixed. `8080` DXP, `8090` sidecar, `8091` broker.

## Track Ownership

| Track | Owns | Directory |
| --- | --- | --- |
| A | Container image, agent sidecar, seed agent workspace | `poc/docker/`, `poc/agent-sidecar/`, `poc/agent-workspace/` |
| B | Broker service, DXP token validation, SSE passthrough | `poc/broker/` |
| C | Chat custom element, OAuth user agent CX, workspace build | `poc/dxp-workspace/` |

## HTTP API

The **broker** exposes exactly these paths under `http://localhost:8091/api`.
The **sidecar** exposes the identical paths under `http://localhost:8090/api`,
minus authentication. The broker is a pass through plus auth. Track C calls only
the broker. Track B calls only the sidecar.

### `GET /api/health`

No auth. Response `200`:

```json
{ "status": "ok", "workspace": "/workspace", "agent": "claude-agent-sdk", "version": "0.3.263" }
```

### `POST /api/sessions`

Creates an agent session. Body optional `{ "title": "string" }`. Response `201`:

```json
{ "sessionId": "uuid", "title": "string", "createdAt": "ISO-8601" }
```

Note the `201` body omits `state`, while `GET /api/sessions` includes it. Callers
treat a newly created session as `idle`.

### `GET /api/sessions`

Response `200`:

```json
[ { "sessionId": "uuid", "title": "string", "createdAt": "ISO-8601", "state": "idle" } ]
```

`state` is one of `idle`, `thinking`, `working`, `deploying`, `error`.

Ordered newest first by `createdAt`. The UI seeds its selection from the head of
this list, so a reload lands on the most recent session rather than the oldest.

### `POST /api/sessions/{sessionId}/messages`

Body `{ "text": "string" }`. Response `202` with empty body. All output arrives on
the SSE stream. Sending while the session is not `idle` returns `409`.

### `GET /api/sessions/{sessionId}/stream`

`text/event-stream`. Never buffered. The broker must disable response buffering and
flush every frame, otherwise the UI appears frozen. Sends a comment heartbeat
`:ping` every 15 seconds so proxies do not drop the connection.

### `POST /api/sessions/{sessionId}/interrupt`

Aborts the in flight turn. Response `204`.

### `GET /api/sessions/{sessionId}/messages`

Replay of retained events for a reload. Response `200`, a JSON array of
**envelopes**, not bare payloads:

```json
[ { "event": "status", "data": { "state": "thinking" } } ]
```

The envelope is required, because bare payloads are not decodable without knowing
which event produced them. Live and replay therefore share one consumer code path.

The stream carries no `id:` field, so a dropped connection cannot resume with
`Last-Event-ID`. Reconnect refetches the whole transcript from this endpoint.

## SSE Event Types

Every frame is `event: <name>` plus `data: <json>`. These are the only names.

| Event | Payload | Meaning |
| --- | --- | --- |
| `assistant` | `{ "text": "string" }` | Assistant text delta, append in order |
| `tool_use` | `{ "id": "string", "name": "string", "input": {} }` | Agent started a tool |
| `tool_result` | `{ "id": "string", "ok": true, "preview": "string" }` | Tool finished; `preview` is truncated to 2000 chars |
| `status` | `{ "state": "thinking" }` | Session state changed; same enum as above |
| `deploy` | `{ "artifact": "string", "state": "started", "path": "string" }` | Deploy lifecycle; `state` is `started`, `copied`, or `failed`. `path` is populated only on `copied` and is an empty string otherwise |
| `result` | `{ "ok": true, "durationMs": 0, "costUsd": 0.0, "turns": 0 }` | Turn complete |
| `error` | `{ "message": "string" }` | Fatal error for the turn |

## Authentication

Track C obtains a DXP OAuth2 token through the `oauth-user-agent` client extension
and sends `Authorization: Bearer <token>` on every broker call.

**Resolved 2026-09-08, verified against a running DXP.** An earlier revision of
this contract recorded this as an unresolved defect on the belief that
`Liferay.OAuth2Client` was the entry point and exposed no token accessor. Both
halves of that were wrong.

There is no `Liferay.OAuth2Client` global at all. The client ships as an ES
module, `@liferay/oauth2-provider-web/client`, listed in the portal import map.
`FromUserAgentApplication` is a top level export of that module and it returns a
Promise. The resolved client exposes `_getOrRequestToken`, which returns the full
token response.

Working chain:

```js
const m = await import('@liferay/oauth2-provider-web/client');
const client = await m.FromUserAgentApplication('liferay-aihub-oauth');
const {access_token} = await client._getOrRequestToken();
```

The resulting Bearer token was verified against
`GET /o/headless-admin-user/v1.0/my-user-account`, the exact endpoint the broker
validates with, returning `200` for `test@liferay.com` on scope
`Liferay.Headless.Admin.User.everything.read`.

The public `fetch` on the client is still unusable here, for the reasons
originally recorded: it parses the body as JSON and rejects on any non 2xx, so it
cannot read the SSE stream, nor tolerate the `202` from `POST messages` or the
`204` from `interrupt`. The raw token is required.

`_getOrRequestToken` remains private API and this remains a POC shim. A
production design needs a public token accessor on the portal side, or a same
origin proxy holding the token server side.

## Filesystem Contract

| Container path | Host path | Mode |
| --- | --- | --- |
| `/workspace` | `poc/agent-workspace` | read write |
| `/liferay/osgi/client-extensions` | `bundles/osgi/client-extensions` | read write |
| `/liferay/deploy` | `bundles/deploy` | read write |
| `/home/node/.claude` | `poc/docker/claude-home` | read write |

The agent workspace sets `liferay.workspace.home.dir=/liferay` in
`gradle.properties`. That is the whole deploy bridge. There is no other
mechanism.

**Corrected 2026-09-08 after live testing.** The Liferay workspace plugin
deploys a client extension to `$LIFERAY_HOME/osgi/client-extensions`, not to
`$LIFERAY_HOME/deploy`. An earlier revision of this contract mounted only
`deploy`, and the built archive was stranded inside the container where DXP
never saw it. Verified build output reads:

```
> Task :client-extensions:aihub-bridge-test:deploy
Files of project ':client-extensions:aihub-bridge-test' deployed to /liferay/osgi/client-extensions
```

`osgi/client-extensions` is therefore the mount that matters. `deploy` stays
mounted because dropping an archive there also works and the agent may choose
that path, but it is not what `gradlew deploy` uses.

`poc/docker/claude-home` is git ignored and seeded by `poc/docker/seed-auth.sh`,
which copies `~/.claude/.credentials.json` in. It must be writable so the agent can
refresh its token.

## Agent Configuration

The sidecar runs `@anthropic-ai/claude-agent-sdk` with `cwd` set to `/workspace`
and permission prompts disabled. The container is the sandbox, and a citizen
developer has no basis to answer a permission dialog. The seed workspace carries
the Liferay skills pack copied from `workspaces/liferay-sample-workspace/.agents`,
which is the reason this POC is not a generic Claude Code wrapper.
