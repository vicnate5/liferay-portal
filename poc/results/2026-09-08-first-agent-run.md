# First Real Agent Run Through The UI — 2026-09-08

A Citizen Developer phrased prompt, typed into the chat widget on the
initializer created site, produced a working client extension and put it live in
DXP without intervention.

## The Prompt

Typed verbatim, with no Liferay vocabulary in it:

> Every button on my site should have fully rounded pill shaped corners. Please
> make that change and put it live on my site.

## Result

| Measure | Value |
| --- | --- |
| Outcome | Success, live in DXP |
| Wall clock | 152 seconds |
| Cost | $0.628 |
| Agent turns | 22 |
| Human intervention | None |
| Artifact | `aihub-button-style`, a `globalCSS` client extension |

Verified in the browser, not inferred from the build:

- CSS served from `/o/aihub-button-style/button-style.<hash>.css`
- `getComputedStyle(button).borderRadius` returns `9999px` on every button
- DXP log: `STARTED aihubbuttonstyle_7.4.13 [1625]`

## What Went Right

**The skills pack fired, and it was legible.** The agent's first action was
loading the `theme-and-design` skill. In the UI that rendered as the plain
language line "Applying the theme-and-design guidance", with no tool name and no
JSON. This is the single best evidence so far for the Risk 4 counter in the
draft: a generic Claude Code wrapper has no `theme-and-design` skill to load, and
would have had to guess at the client extension type.

**It picked the right artifact type unprompted.** Nobody said "global CSS" or
"client extension". The user asked for rounded buttons.

**The deploy bridge carried it.** Built in the container, landed in the host
bundle, hot deployed by DXP, visible on the page.

## What Went Wrong, And It Matters More

**The agent could not verify its own work, and said so.** Its closing message:

> I don't have working REST/log access from this container to the running DXP
> (every path I probed on port 8090 returned an identical generic 404, and
> there's no `bundles/`/`catalina.out` mounted here) so I can't independently
> confirm DXP actually hot-deployed it and buttons are rendering pill-shaped.
> Please do a quick visual check on your site.

The honesty is correct behavior. The situation is a design defect. The container
has no route to DXP, so the agent cannot close its own loop, and it wasted turns
probing port 8090, which is its own sidecar, in the belief it might be DXP.

This matters because a non technical user cannot be the agent's verification step.
Asked "did that work?", they have no better answer than the agent does, so every
failed attempt becomes a round trip through a human who cannot diagnose anything.

**It is unbuilt rather than hard, and an earlier draft of this file overstated it
as structural.** Two additions close it, neither novel:

1. A headless browser inside the container, so the agent can load the page it
   changed and inspect the rendered result directly.
2. A service exposing the running product's logs to the container, so deployment
   failures are visible to the agent rather than only to a person.

With those, the agent checks and corrects its own work before returning, which is
the normal agentic loop rather than a new capability.

**The container was running a stale sidecar.** The `deploy` event reported
`path: /liferay/deploy/aihub-button-style.zip` while the artifact was actually at
`/liferay/osgi/client-extensions/`. Cause: after patching `server.js` for the
earlier mount fix, the container was recreated from the unchanged image rather
than rebuilt. `docker compose up -d --force-recreate` does not rebuild. Image
rebuilt.

## Cost Note

$0.628 for one styling change, against the earlier floor of $0.167 for a one word
answer. The 22 turns were dominated by orientation and by the failed attempts to
find DXP. Closing the verification loop would likely cut both the turns and the
cost, which makes it a cost fix as much as a correctness fix.
