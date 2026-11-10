# Patcher 2.3 compatibility migration

## Motivation and root cause

The v0.6.0 bundle is compiled against Morphe Patcher 1.15.1. Manager 2.5.0
uses Patcher 2.3.0; when another selected bundle uses Patcher 2.x, exact-string
fingerprinting behavior applies to the older bundle too, producing the reported
compatibility warning. This is separate from ARMv7-only input failing to install
on a 64-bit-only device.

Upstream documents version-driven behavior in
https://github.com/MorpheApp/morphe-patcher/blob/v2.3.0/docs/5_patcher_changes.md .
Do not suppress the warning or edit the MPP manifest to claim an untested version.

## Planned change

Build against actual Patcher 2.3.0 and the compatible official settings plugin.
Audit selective resource decoding and exact-string matching; retain all app pins,
public patch selection/defaults, original input identity and native libraries.
Use deprecated context APIs where still supported instead of unrelated API rewrites.
No ABI support expansion is part of this change.

## Acceptance and regression frame

- Failing case: old v0.6.0 bundle emits the compatibility warning under Patcher 2.3.0.
- New bundle: actual dependency and MPP manifest declare 2.3.0, no old-bundle warning.
- CI-equivalent: Java21/SDK36, npm CI/tests, Gradle tests/buildAndroid/catalog.
- Positive: PiTT clean ARM64 source, Google/local selections and combined output;
  GPS JoyStick clean Play/Unlocked with blank Maps key; app-owned guard tests for others.
- Nearby: PiTT supplied ARMv7 XAPK can patch, but stays ARMv7; no installation promise.
- Guards: wrong-version/already-patched input must still reject; compare native/asset
  preservation and patched method behavior instead of treating build success as phone acceptance.
- Device install/startup/login and disappearance of the warning in Manager UI remain
  user acceptance; do not claim a device run from desktop output alone.

## Results

- Updated the Patcher pin from 1.15.1 to 2.3.0 and official settings plugin
  from 1.3.4 to 2.1.0. Migrated the catalog generator from `Patch<*>` to the
  non-generic `Patch` API. Public app patches/defaults are unchanged.
- Audited resource access: decoded-resource patches request individual XML files;
  the only directory listing is Windy raw assets, not a decoded `res/` tree.
  No legacy raw-string Fingerprint declarations occur in this bundle.
- `npm ci --include=dev` and all 9 release-tooling tests passed.
- Diagnostic Kotlin compilation of all production source against the new desktop
  engine API passes. This is a source compatibility check, NOT an official MPP
  build or a replacement for Gradle/extension tests.
- The v0.6.0 baseline still applies both PiTT patches on the modern desktop engine;
  resulting DEX and native entries match the previous qualified test output.
  Old bundle alone retains old behavior; the supplied Manager screenshot is the
  mixed-bundle warning reproduction.
- Formal Gradle build is blocked locally by GitHub Maven registry authentication
  (HTTP401). A CI validation route must be agreed before pushing this branch.
  No candidate MPP, completed APK regression, PR, merge or release is claimed yet.
