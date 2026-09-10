# AI Hub Workspace POC — Agent Container (Track A)

The container runs the agent sidecar on `:8090` and hosts the Liferay Workspace the
agent writes code in. It implements the sidecar half of `poc/CONTRACT.md`.

## Layout

| Path in container | Source | Purpose |
| --- | --- | --- |
| `/opt/sidecar` | `poc/agent-sidecar` | Node 22 sidecar, `node server.js` |
| `/workspace` | bind mount of `poc/agent-workspace` | the Liferay Workspace the agent edits |
| `/opt/seed-workspace` | baked copy of `poc/agent-workspace` | seed used for the Gradle pre-warm |
| `/liferay/deploy` | bind mount of `/opt/dev/projects/github/bundles/deploy` | DXP deploy folder |
| `/home/node/.claude` | bind mount of `poc/docker/claude-home` | agent credentials, writable so the token can refresh |

The container runs as the non-root `node` user, uid 1000, which matches the host
user, so the bind mounts need no permission fixing.

## First Run

```bash
./seed-auth.sh
docker compose build
docker compose up -d
curl http://localhost:8090/api/health
```

`seed-auth.sh` copies `~/.claude/.credentials.json` into `claude-home/`. It is
idempotent and exits non-zero with a clear warning when the source is missing.
Without it every agent turn fails unauthenticated.

## Gradle Pre-Warm And Expected Build Times

The image build downloads the Gradle 8.5 distribution and resolves the Liferay
Workspace plugin into `/home/node/.gradle` in an image layer, so the agent does not
pay that cost on its first deploy. The warm runs `gradlew projects` against the seed
workspace and is deliberately fail soft: a network problem at image build time
prints a warning rather than failing the build.

Measured on this machine with a cold Gradle home:

| Step | Cold | Warm |
| --- | --- | --- |
| Gradle 8.5 distribution download plus workspace plugin resolve | about 50 seconds | 0, baked into the image |
| First `:client-extensions:<name>:deploy` of a static client extension | about 14 seconds | about 6 seconds |

A client extension that compiles Java or runs an npm build is slower on its first
run, because it resolves the target platform BOM and pulls an npm tree. Budget five
to ten minutes for that first build and seconds for later ones. That case is not
covered by the pre-warm and is untested in this POC.

## Deploy Path Caveat

`gradlew :client-extensions:<name>:deploy` writes a client extension zip to
`${liferay.workspace.home.dir}/osgi/client-extensions`, not to
`${liferay.workspace.home.dir}/deploy`. The contract mounts only `deploy`. The
Dockerfile therefore points `/liferay/osgi/client-extensions` at `/liferay/deploy`
with a symlink, so the artifact lands in the mounted folder. See the Track A report
for the detail.

## Operating

```bash
docker compose logs -f aihub-workspace   # sidecar log, one line per session event
docker compose restart aihub-workspace   # sessions are in memory and are lost
docker compose down
```

Sessions are held in memory only. A restart drops every session and its replay
buffer. That is acceptable for the POC and is the first thing to change if this
becomes real.
