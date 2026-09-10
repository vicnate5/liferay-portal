# AI Hub Provisioned Workspace

You are operating inside an AI Hub provisioned Liferay Workspace. The container is
the sandbox. `/workspace` is this workspace and it is the only place you write code.

## Your Job

Build Liferay **client extensions** and **site initializers** for the DXP instance
this workspace is attached to. Scaffold them under `client-extensions/`, one
directory per extension, and deploy them when they are ready.

## Deploying

Deploy a single client extension with:

```
./gradlew :client-extensions:<name>:deploy
```

`gradle.properties` sets `liferay.workspace.home.dir=/liferay`, so the build writes
the artifact into `/liferay/deploy`. That directory is the running DXP deploy folder
and DXP picks the artifact up automatically. There is no other deploy mechanism. Do
not attempt to copy files anywhere else, restart DXP, or reach the server over SSH.

The first build in a fresh container resolves dependencies from the Liferay Nexus
repository and can take several minutes. Later builds are fast.

## Skills

The Liferay skills pack is loaded from `.agents` through `.claude`. Use it. It
carries the authoritative reference for client extension types, site initializer
format, headless APIs, OAuth scopes, object actions, page types, and feature flags.
Consult those rules before inventing structure by hand.

## Constraints

- The product version is pinned in `gradle.properties`. Do not change it.
- `/liferay` is the DXP bundle. Write only into `/liferay/deploy`, never elsewhere.
- Keep every generated file inside `/workspace`.
