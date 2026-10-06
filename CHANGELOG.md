## [1.0.0](https://github.com/pigfoot/morphe-patches/compare/v0.0.1...v1.0.0) (2026-10-06)

### ⚠ BREAKING CHANGES

* RailsGo now retains its original package unless Change package name is selected. Existing .morphe installs must explicitly select package renaming and keep the same local signing key. Sideload remains required for standard and Shizuku installs; root mount is unqualified.

### 🐛 Bug Fixes

* make RailsGo package renaming opt-in ([c0b16f8](https://github.com/pigfoot/morphe-patches/commit/c0b16f83c6e03e59ec0990a312b7b22514137e0b))

## Pigfoot Patches v0.0.1

Initial release of the rebuilt repository using the official Morphe Gradle layout.

- RailsGo 1.25.2 (156), arm64-v8a APKS only.
- Change package name: configurable package with provider, permission and link isolation. The original app display name is preserved.
- Sideload startup compatibility remains a required dependency.
- Scoped bus-update completion retains loaded reward metadata, fallback behavior and duplicate-callback protection.
- Java extension with compile-only declarations excluded from the shipped bundle.
- English documentation and semantic-release automation for subsequent releases.

Keep the default package name and the same local signing key to update an existing patched installation. Device startup and live bus updates require phone validation after the Java extension migration.
