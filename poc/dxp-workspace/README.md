# AI Hub Workspace POC: DXP Client Extensions

Track C of the POC. This workspace holds the two client extensions that put the
agent in front of a non developer inside DXP.

| Client extension | Type | Purpose |
| --- | --- | --- |
| `liferay-aihub-chat-ui` | `customElement` | React chat widget that talks to the broker |
| `liferay-aihub-oauth` | `oAuthApplicationUserAgent` | Issues the DXP access token the widget sends to the broker |

The widget never speaks to the sidecar or to the container. It speaks only to the
broker on port `8091`, exactly as `poc/CONTRACT.md` describes.

## Prerequisites

1. A local DXP bundle at `/opt/dev/projects/github/bundles`, which is what
   `liferay.workspace.home.dir` in `gradle.properties` points at.
2. The sidecar running on `:8090` and the broker running on `:8091`. Both belong
   to tracks A and B.
3. Node 20 or later. The workspace uses npm as the package manager.

## Build

```bash
cd poc/dxp-workspace
./gradlew :client-extensions:liferay-aihub-chat-ui:build \
          :client-extensions:liferay-aihub-oauth:build
```

The chat UI build runs `npm install` and then esbuild. React, React DOM, and every
`@clayui` package are marked external, because DXP already serves them through its
JavaScript import maps. The shipped bundle is therefore small and picks up the
Clay version of whichever DXP it lands on.

## Deploy

```bash
cd poc/dxp-workspace
./gradlew deploy
```

`deploy` writes both client extension archives into
`/opt/dev/projects/github/bundles/deploy`, and the running DXP hot deploys them.
Watch the DXP console for the two archives to register.

## Add The Widget To A Page

1. Sign in to DXP as an administrator.
2. Open the page you want, switch to edit mode, and open the Fragments and
   Widgets panel.
3. Find **AI Hub Workspace Chat** under the **Client Extensions** category.
4. Drag it onto the page and publish.

The widget is also reachable on its own at `/o/aihub-chat` through the friendly
URL mapping declared in `client-extension.yaml`.

## Configure

### Broker Base URL

The default lives in exactly one place, `src/config.ts`:

```ts
export const DEFAULT_BROKER_BASE_URL = 'http://localhost:8091';
```

Override it per page without a rebuild by setting the `broker-url` attribute on
the element:

```html
<liferay-aihub-chat-ui broker-url="http://broker.internal:8091"></liferay-aihub-chat-ui>
```

If you change the broker address permanently, change it in three places that must
agree:

1. `src/config.ts`, for the widget.
2. `.serviceAddress` in `client-extensions/liferay-aihub-oauth/client-extension.yaml`,
   because that value becomes the `homePageURL` of the OAuth application and the
   portal OAuth client refuses to send a token to any other origin.
3. The CORS allowed origin on the broker, which must name the DXP origin.

### OAuth

`client-extensions/liferay-aihub-oauth/client-extension.yaml` registers a user
agent application whose external reference code is its yaml key,
`liferay-aihub-oauth`. The widget resolves it by that code in `src/config.ts`
through `OAUTH_USER_AGENT_ERC`.

The scope granted is `Liferay.Headless.Admin.User.everything.read`, which is the
minimum the broker needs. The broker validates a token by calling
`GET /o/headless-admin-user/v1.0/my-user-account` with it.

If the token cannot be obtained, the widget degrades to anonymous mode: it drops
the `Authorization` header, shows one informational notice, and keeps working.
That is the mode to use when the broker runs with authentication disabled.

## How The Stream Is Read

`EventSource` cannot send an `Authorization` header, and putting a token in a
query string leaks it into access logs and browser history. The widget therefore
calls `fetch` on `GET /api/sessions/{id}/stream`, reads
`response.body.getReader()`, and decodes the wire format in `src/lib/sse.ts`.
Comment frames such as `:open` and `:ping` are discarded by the parser and never
reach the application.

`src/lib/events.ts` folds frames into the transcript. Assistant text arrives as
small deltas and is appended into a single bubble. A `tool_result` merges into
the activity line opened by its `tool_use`. Repeated `deploy` frames for one
artifact update a single card rather than stacking three.

## Verify Against A Live Broker

The production parser and the production reducer can be exercised from Node,
with no browser and no DXP:

```bash
cd client-extensions/liferay-aihub-chat-ui
npm run verify:broker -- "http://localhost:8091" "Reply with the single word READY and stop."
```

It creates a session, attaches to the stream, sends the prompt, prints every
decoded frame, and prints the transcript the widget would render.

## Source Layout

```
client-extensions/liferay-aihub-chat-ui/
  client-extension.yaml     customElement declaration
  scripts/verify-broker.ts  live broker harness for the parser and the reducer
  src/config.ts             broker URL default, OAuth reference code, element name
  src/lib/sse.ts            SSE frame parser, free of React and of the DOM
  src/lib/events.ts         contract event types, transcript reducer, tool wording
  src/lib/broker.ts         HTTP client for the broker
  src/lib/auth.ts           OAuth token acquisition and anonymous fallback
  src/lib/liferay.ts        the portal globals this widget relies on
  src/App.tsx               session list, transcript, prompt box, state wiring
  src/components/           Clay based presentation
  src/styles/index.css      layout only, every value from a Liferay design token
client-extensions/liferay-aihub-oauth/
  client-extension.yaml     oAuthApplicationUserAgent declaration
```
