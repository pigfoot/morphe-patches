# Pigfoot Patches

App-scoped patches built with the official Morphe Gradle template.

## Installation

Add `https://github.com/pigfoot/morphe-patches` as a source in Morphe Manager.
Select the clean RailsGo (台灣鐵道通) **1.25.2 (156), arm64-v8a APKS**.
Selecting the bus-update patch automatically includes package isolation and sideload compatibility.
Keep the default package and the same local signing key to update an existing patched installation.
The original app display name is preserved. Phone startup and live bus updates still require device validation.

## Patches

<!-- PATCHES_START -->
Release automation generates the patch list here.
<!-- PATCHES_END -->

## Repository layout

- `patches/src/main/kotlin/app/pigfoot/patches/railsgo/`: version-pinned app patches.
- `extensions/railsgo/src/main/java/`: injected runtime helper.
- `extensions/railsgo/stubs/`: compile-only app/SDK declarations, never bundled.
- `patches/src/test/`: fallback, reward metadata, deduplication and callback failure tests.
- `gradle/`: wrapper and version catalog.
- `.github/workflows/`: verification and semantic-release automation.

## Build

Requires Java 21, Android SDK 36, and a GitHub token with `read:packages` for Morphe dependencies.
Set `GITHUB_ACTOR` and `GITHUB_TOKEN`, or configure `gpr.user` and `gpr.key` in your **user-level** Gradle properties. Never commit credentials.

```sh
./gradlew :patches:test :patches:buildAndroid :patches:generatePatchesList
```

The bundle is written to `patches/build/libs/patches-<version>.mpp`.

## Versioning

The initial release is **v0.0.1**. Subsequent releases use semantic-release:

- `.releaserc` defines release branches and commit rules.
- `fix:` / `perf:` / `bump:` increments the patch version.
- `feat:` increments the minor version; breaking changes increment the major version.
- `chore:` / `docs:` alone does not publish a release.
- `dev` publishes prereleases; merging `dev` into `main` publishes stable releases.
- Automation updates `gradle.properties`, changelog, Manager metadata and patch list together.

Do not manually edit generated metadata or rewrite release tags. The initial v0.0.1 is bootstrapped once; future releases follow the template workflow.

## License

[GNU General Public License v3.0](LICENSE). Build/release scaffolding and patch-list generation derive from the [Morphe template](https://github.com/MorpheApp/morphe-patches-template).
