# CamScanner implementation and validation frame

## Motivation and scope

Add independently selectable CamScanner feature patches to the existing Gradle bundle without changing RailsGo behavior. Target clean com.intsig.camscanner 7.26.5.2609160000 (72651). A full APKM input includes base, cloud feature modules and OCR assets. Donor variants are research references, not trusted merge baselines.

## Intended changes

- Local feature status / optional folder cap, without claiming server credits or cloud quotas.
- Firebase Analytics/Crashlytics/Performance/Sessions suppression with explicit options and runtime re-enable defenses.
- App logging suppression, covering local output and network dispatch/reconnect/cache replay.
- Ads suppression through app-owned ad ingress and reviewed manifest candidates; preserve login/Billing/WorkManager by default.
- Offline mode through removal of all INTERNET declarations; depend on ads suppression and retain actual platform connectivity behavior.
- Separate optional scanner initialization compatibility, if needed, guarded by exact native bytes/ABI. No crop-fix or repeat-reward claim without device evidence.

## Guards / rejection cases

Exact package/version/code; clean class/method fingerprints; no donor bootstrap classes or injected fields; original scanner bytes where native changes apply; fail on missing/multiple matches or already modified inputs. Validate all baseline fingerprints before any public patch mutates a shared class. Preserve the original app name/package by default.

## Success criteria and tests

Compile using the official Gradle plugin. Run existing RailsGo tests plus option/dependency/input-policy tests. Apply the real bundle to a clean independently merged APKM and inspect exact expected method/manifest deltas, native/assets/module retention, and unchanged app identity. Verify modified/wrong-version inputs reject. Test independent selection and ad-support/clean/offline combinations. No APK signature, phone startup, genuine ad outcome, crop, or zero-telemetry claim from build/static tests alone.

## Runtime qualification pending

Java/native telemetry and stored reports, foreground/background and cached logger replay, normal scan/import/OCR/crop/export, offline socket/WebView/other-service boundaries, optional cloud integrations and actual advertising completion. Keep unqualified patches disabled by default. No automatic stable release or private APK upload.

### Persisted Crashlytics collection overrides

The all-telemetry policy also guards the two qualified internal collection predicates
in DataCollectionArbiter, because saved SDK overrides can take precedence over manifest
collection defaults. It does not erase user preferences or promise no local native crash
files. Manual upload, public re-enable setters and wrapper paths are handled separately.

## Verified experimental bundle (2026-10-14)

Source commit: `9eaa3469aa1e379b4d233520691fb1eb826e3eb8`.
Official [Verify CI](https://github.com/pigfoot/morphe-patches/actions/runs/38446587730)
and [feature-branch artifact CI](https://github.com/pigfoot/morphe-patches/actions/runs/38446581834)
succeeded. The Gradle test reports contain 24 passing tests, including existing RailsGo tests.
No stable release was published.

Bundle SHA-256: `da99937cf2c41cfccd25c05af660e15a7b900cda2cef1f230ae7c45138970a51`.
The artifact retains the upstream build version; the hash/source commit identify this experimental build.

### Selection and options

Six public CamScanner patches are independently listed and disabled by default:

- **CamScanner local features**: `folderLimit` = 0 (original limit) or 5000.
- **CamScanner disable Firebase telemetry**: `scope` = `all` or `analytics`.
- **CamScanner disable app logging**: `localOutput` = true or false.
- **CamScanner disable ads**: `manifestCleanup` = true or false.
- **CamScanner offline mode**: includes ads suppression through a dependency; does not imply local features, Firebase suppression or scanner compatibility.
- **CamScanner scanner initialization compatibility**: separately opt-in, qualified ARM scanner library patches only.

For an ad-support combination, leave ads suppression and offline mode unselected.
For clean, select ads suppression but not offline mode.
For offline, select offline mode (its ads dependency is resolved automatically).
Select local features, Firebase suppression, app logging and scanner compatibility separately as needed.
These are combinations, not exclusive modes. Ad rewards are not bypassed or certified.
Do not treat local feature status as full subscription/server-feature coverage.

### Actual output validation

Applied the downloaded official bundle with Morphe Desktop 1.18.1 / Patcher 1.15.1
using its normal STRIP_FAST bytecode mode to the independently APKM-merged pristine input.
All three combinations patched and rebuilt successfully, unsigned. The qualified combinations
included local features with `folderLimit=5000`, Firebase `scope=all`, app logging
with `localOutput=true`, and optional scanner compatibility. Clean and offline
also applied ads suppression with `manifestCleanup=true`.

All three output ZIP CRC checks passed. Each preserves the original package/name/version,
103,599 class definitions without duplicates, all 1,037 asset/native file entries,
the mobile OCR model and feature-module classes. Only the two qualified scanner
libraries differ among native/assets; all other file hashes match the merged baseline.
VK/WeChat/Billing/WorkManager components are retained.

The pinned-class change sets are exactly 13 classes for ad-support and 107 for clean/offline.
Independent output-bytecode checks also confirm 14 critical method prefixes per output: Analytics entry points, Crashlytics reporting/upload and collection setters, both persisted-override arbiter gates, Performance setters, and the requested folder cap. Firebase manifest deactivation flags are present. Ad-support and clean retain the two
original INTERNET declarations; offline has zero. Real negative application tests reject
the supplied modified donor, an already patched output, and a wrong-version fixture.
The wrong-version test explicitly bypasses the CLI compatibility check to exercise the
patch's own fail-closed guard.

Static/build checks are not Android runtime qualification. Outputs were not signed,
installed or launched. Startup, crop/OCR/export behavior, SDK native reports, actual
ad callbacks and external-service/network boundaries still need device tests.
