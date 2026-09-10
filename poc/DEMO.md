# AI Hub Workspace POC — Demo Script

The plumbing is the cheap part. This document is the instrument that measures the
expensive part: whether a Citizen Developer prompt produces a deployable artifact
without developer intervention.

Run this exactly as written. Do not coach the agent. Do not repair its output by
hand. The point is to record what actually happens.

## Setup

1. Local DXP bundle running at `http://localhost:8080` from
   `/opt/dev/projects/github/bundles`.
2. Container running: `docker compose -f poc/docker/docker-compose.yml up -d`
3. Broker running on `:8091`.
4. Chat UI widget deployed and placed on a DXP page.
5. `bundles/deploy` empty at the start, so the artifact is unambiguous.

## The Prompt

Typed verbatim into the chat UI by a user who has never opened a terminal:

> I run a conference and I need to keep track of the talk proposals people send
> in. For each proposal I want the speaker name, their email, the talk title, a
> short abstract, and whether we have accepted it yet. Set that up in my site and
> give me a page where I can see them all.

Nothing else. No follow up unless the agent asks a direct question, in which case
answer in the same non technical register and record that you did.

## Success Criteria

Grade each independently. Partial credit is the useful signal here.

| # | Criterion | Pass condition |
| --- | --- | --- |
| 1 | Intent understood | Agent identifies this as an Object plus a page, without being told those words |
| 2 | Artifact produced | A `site-initializer` client extension appears in `/workspace/client-extensions` |
| 3 | Object modelled | Object definition includes all five fields with sane types, including a boolean or picklist for acceptance |
| 4 | Deploy attempted | Agent runs `gradlew deploy` unprompted |
| 5 | Artifact lands | A `.zip` appears in `bundles/deploy` and DXP consumes it |
| 6 | DXP reflects it | Object is visible under the Objects admin and the page renders |
| 7 | Unattended | Zero developer intervention from prompt to criterion 6 |
| 8 | Legible progress | A non technical observer watching the UI can tell what is happening and when it finished |

Criterion 7 is the one that decides whether the proposal's Citizen Developer
persona survives contact with reality. Criterion 8 decides whether the UI is worth
building at all versus shipping a CLI.

## Known Traps

These are documented failure modes from prior work on this machine. If the run
dies on one of them, that is an environment problem, not an agent capability
problem, and must be recorded as such.

1. **Site initializer deactivates seeded users.** Users imported through
   `user-accounts.json` land Inactive, so subsequent Basic auth calls return 401.
   The fix is a PATCH to Active after import.
2. **Headless Basic auth 403.** A 403 with valid credentials is the admin
   `passwordReset` and `agreedToTermsOfUse` flags, not Service Access Policy.
   Three portal-ext properties must be set before database initialisation.
3. **First Gradle build is slow.** If the image cache did not warm, the first
   deploy stalls for minutes and looks like a hang. Time it and record the number.
4. **Hot deploy latency.** Client extension pickup runs tens of seconds. Do not
   call criterion 5 failed until DXP has had a full minute.

## Recording

Capture, into `poc/results/run-<date>.md`:

- Wall clock from prompt submitted to criterion 6 satisfied
- Token cost reported by the `result` SSE event
- Number of agent turns
- The pass or fail grade for all eight criteria
- Every point at which a human intervened, and why
- A screen recording of the UI, since criterion 8 cannot be graded from logs

## Interpreting The Result

- **Criteria 1 to 6 pass, 7 fails**: the plumbing works and the agent works, but
  the persona does not. That points at Phase 1 of the draft, the bounded scope
  generator, rather than a full Workspace.
- **Criterion 7 passes, 8 fails**: build the pipe, rethink the UI.
- **Criteria 2 to 5 fail**: the deploy bridge is wrong. This is the cheap failure
  and the one to fix first.
- **Everything passes on the first attempt**: raise the difficulty before drawing
  any conclusion. One green run on a rehearsed prompt is not evidence.
