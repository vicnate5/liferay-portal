# AI Hub Workspace POC

Proves that a Liferay DXP user interface can drive a Claude coding agent running
in a dedicated Docker container, and that artifacts the agent builds land in a
DXP install outside the container.

Source proposal: `inbox/ai-hub-workspace-draft.md` in the knowledge base
workspace. Read `CONTRACT.md` before changing anything that crosses a service
boundary, and `DEMO.md` before drawing conclusions about agent capability.

## Layout

| Path | What it is | Port |
| --- | --- | --- |
| `agent-sidecar/` | Node service wrapping `@anthropic-ai/claude-agent-sdk` | 8090 |
| `docker/` | Image and compose for the agent container | |
| `agent-workspace/` | Liferay Workspace the agent works in, carrying the skills pack | |
| `broker/` | Spring Boot service, DXP OAuth2 check plus SSE passthrough | 8091 |
| `dxp-workspace/` | Chat custom element and OAuth user agent client extensions | |
| `results/` | Verification records and demo runs | |

The local DXP bundle is `/opt/dev/projects/github/bundles` on port 8080.

## Run It

```bash
# 1. Seed the container with host Claude credentials. Idempotent.
bash docker/seed-auth.sh

# 2. Build and start the agent container.
docker compose -f docker/docker-compose.yml up -d --build

# 3. Confirm the sidecar answers.
curl -s http://localhost:8090/api/health

# 4. Start the broker. Drop the auth flag once DXP is running.
cd broker && ./gradlew bootRun --args='--broker.auth.enabled=false --server.port=8091'

# 5. Build and deploy the DXP side extensions.
cd dxp-workspace && ./gradlew deploy
```

Then add the AI Hub Workspace widget to a page in DXP.

## Claude Credentials Go Stale

The container runs on a copy of the host Claude token, and that token lasts about
eight hours. When it lapses, every turn fails in the UI with `Failed to
authenticate: OAuth session expired and could not be refreshed`. Re-seed and
recreate:

```bash
bash docker/seed-auth.sh
docker compose -f docker/docker-compose.yml up -d
```

Compose mounts `.credentials.json` read only. Host and container share a single
refresh token, so whichever refreshes first rotates it and invalidates the other.
The container always lost that race and wrote back an empty record with
`expiresAt` of 0, which never recovers on its own. Read only keeps the container
a pure consumer, so the failure now arrives at real expiry rather than an hour in.

Removing the eight hour ceiling means giving the container its own credential:
set `ANTHROPIC_API_KEY` in the compose environment instead of mounting the file.
An API key does not expire and bills the API account rather than the
subscription.

## Verified Working

Recorded with evidence in `results/2026-09-08-plumbing-verification.md`.

1. Sidecar serves the contract and runs real agent turns in `/workspace`
2. The skills pack is mounted and loaded, so the agent has Liferay knowledge
3. SSE emits every contract event name with the right shape
4. The broker proxies without buffering, confirmed by frame timestamps
5. A client extension built inside the container reaches the host bundle
6. Both DXP side extensions build, producing deployable archives

## Not Yet Verified

1. DXP consuming a deployed artifact. Needs the bundle running.
2. The OAuth token flow. See the known defect in `CONTRACT.md`; the current
   approach reads a portal session storage key and is expected to be brittle.
3. The widget rendering in a browser.
4. The `DEMO.md` run, which is the only thing that speaks to whether a citizen
   developer prompt actually produces a working artifact.

## Two Findings That Matter More Than The Plumbing

**Cost has a floor set by the skills pack, not by user intent.** A one word
answer cost $0.167. The pack loads into context every turn. The pack is the
reason this is not a generic Claude Code wrapper, and it is the reason per turn
cost is high and flat. Draft Risk 4 and Risk 6 are the same fact from two sides.

**A green build is not a delivered artifact.** The first client extension build
reported `BUILD SUCCESSFUL` while the archive sat stranded inside the container,
because the workspace plugin writes to `osgi/client-extensions` rather than
`deploy`. Any future work on artifact delivery, which is draft Risk 2, should
assume this class of silent failure is the normal case.
