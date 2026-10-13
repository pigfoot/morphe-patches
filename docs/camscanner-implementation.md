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
