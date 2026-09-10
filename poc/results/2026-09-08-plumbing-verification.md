# POC Plumbing Verification — 2026-09-08

Status: the pipe works. The agent capability question in `DEMO.md` is not yet
answered, because that run requires DXP to be up.

## Verified

| # | Claim | Evidence |
| --- | --- | --- |
| 1 | Container builds and sidecar serves the contract | `GET /api/health` returns `{"status":"ok","workspace":"/workspace","agent":"claude-agent-sdk","version":"0.3.263"}` |
| 2 | Agent runs in the provisioned workspace with the skills pack | Agent listed `/workspace` and both `.agents/` and `.claude/` were present |
| 3 | SSE emits the contract event names | `status`, `tool_use`, `tool_result`, `assistant`, `result` all observed with correct payload shapes |
| 4 | Broker proxies without buffering | Frames timestamped 31.5s, 33.6s, 35.5s, 36.1s, then `:ping` at 45.5s and 59.0s |
| 5 | Broker auth path works | Broker test suite passes, including the 401 case and incremental streaming |
| 6 | Deploy bridge carries an artifact to the host | `aihub-bridge-test.zip` built in the container appeared at `bundles/osgi/client-extensions/` on the host |
| 7 | Gradle cache warm is effective | Client extension build completed in 8 to 10 seconds, not minutes |

## Contract Defect Found And Fixed

The original contract mounted `bundles/deploy` to `/liferay/deploy` and asserted
that `gradlew deploy` writes there. That is wrong. The Liferay workspace plugin
writes client extensions to `$LIFERAY_HOME/osgi/client-extensions`:

```
> Task :client-extensions:aihub-bridge-test:deploy
Files of project ':client-extensions:aihub-bridge-test' deployed to /liferay/osgi/client-extensions
```

Because only `deploy` was mounted, the first successful build reported
`BUILD SUCCESSFUL` while the archive was stranded inside the container and the
host bundle stayed empty. This is worth recording because it is the exact failure
shape that would have been reported as working: green build, no artifact.

Fix: mount `bundles/osgi/client-extensions` as well, and correct the sidecar
default `AIHUB_DEPLOY_DIR`. Both mounts are now present.

## Cost Finding, Relevant To Draft Risk 6

Two trivial turns against the provisioned workspace:

| Prompt | Cost | Turns |
| --- | --- | --- |
| List the files in the workspace root | $0.2492 | 2 |
| Say the single word READY | $0.1671 | 1 |

A single word answer cost 17 cents. The driver is the Liferay skills pack loading
into context on every turn. This is the central tension in the proposal: the
skills pack is the differentiator against a generic Claude Code wrapper
(draft Risk 4), and it is also what makes per turn cost high and hard to predict
(draft Risk 6). The two risks are the same fact seen from two sides.

Implication for pricing: per turn cost floor is set by pack size, not by user
intent. A citizen developer asking a trivial question pays close to what a
complex build costs. Any buy-up pricing model needs to account for that floor.

Mitigation worth testing later: lazy skill loading, so only the relevant skill
enters context. Not attempted in this POC.

## Still Unverified

1. DXP consuming the deployed artifact. Requires the bundle running.
2. OAuth user agent registration and real bearer token flow. Requires DXP.
3. The chat UI end to end.
4. The `DEMO.md` run, which is the only thing that speaks to agent capability
   rather than plumbing.

## Addendum, Same Day: Full Stack Verified In A Browser

The bundle was booted and the whole chain exercised through the DXP user
interface. Everything in the "Still Unverified" list above except the `DEMO.md`
run is now verified.

### Booting The Bundle Took Three Fixes

1. `-XX:ActiveProcessorCount=2` added to `setenv.sh`. Sixteen visible cores fail
   Virtual Cluster license validation. Backup at `setenv.sh.bak-aihub`.
2. No database existed. `ant database-reset-container` created `localhost_mysql`
   on 3307 with a fresh `lportal`. No container existed beforehand.
3. License staged from `blade-play/CONFIGS/saved-license/`.

Both first login gates then appeared, Terms of Use and password reminder, which
matches the previously recorded headless auth blocker. Admin reminder answer set
to `test`.

### Site Initializer

A `siteInitializer` client extension, `liferay-aihub-site-initializer`, creates a
dedicated **AI Hub Workspace** site with a **Workspace** page carrying the chat
widget. Log confirms `Initialized AI Hub Workspace for group 37675 in 187 ms`.

The widget is placed by the portable token form rather than a captured portlet
id:

```json
{
	"definition": {
		"widgetInstance": {
			"widgetConfig": {},
			"widgetName": "[$CLIENT_EXTENSION_ENTRY_ERC:LXC:liferay-aihub-chat-ui$]"
		}
	},
	"type": "Widget"
}
```

Worth noting for later: real workspaces in `liferay-portal` contain both forms,
and several LMS initializers hardcode instance specific portlet ids such as
`ClientExtensionEntryPortlet_57868206215768_LXC_liferay_lms_courses_list`. Those
are not portable across installs. The token form is.

### The OAuth Defect Was Mine, And It Is Fixed

The earlier finding that DXP OAuth2 could not supply a token was wrong in both
of its premises. There is no `Liferay.OAuth2Client` global; the client is an ES
module in the portal import map, `@liferay/oauth2-provider-web/client`. Its
`FromUserAgentApplication` export returns a Promise, and the resolved client does
expose a token accessor, `_getOrRequestToken`.

The token was verified against `GET /o/headless-admin-user/v1.0/my-user-account`,
the exact endpoint the broker validates with: `200`, `test@liferay.com`, scope
`Liferay.Headless.Admin.User.everything.read`.

The lesson generalises past this POC. The failure was diagnosed from reading the
client's public surface, concluded to be a blocker, and written into the contract
as unresolved. Ten minutes against a running portal disproved it. Nothing about
this stack should be declared impossible without a booted DXP in front of it.

### End To End, With Authentication Enforced

Broker restarted with `broker.auth.enabled=true`. It returns `401` with no token
and `200` on the exempt health endpoint. On the initializer created page, with
no anonymous fallback in play:

| Step | Evidence |
| --- | --- |
| Token acquired | Degraded mode banner absent |
| Broker accepted it | Session list populated with auth enforced |
| Agent round trip | Prompt "Reply with exactly AUTHENTICATED OK" returned `AUTHENTICATED OK`, `Done in 2s` |

Chain proven: browser, DXP page, custom element, DXP OAuth2 token, broker,
token validation against DXP, container, Claude agent, and back.

### Remaining

The `DEMO.md` run. It is the only outstanding item, and the only one that speaks
to agent capability rather than plumbing.
