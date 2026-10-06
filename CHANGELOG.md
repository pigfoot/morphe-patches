## Pigfoot Patches v0.0.1

Initial release of the rebuilt repository using the official Morphe Gradle layout.

- RailsGo 1.25.2 (156), arm64-v8a APKS only.
- Change package name: configurable package with provider, permission and link isolation. The original app display name is preserved.
- Sideload startup compatibility remains a required dependency.
- Scoped bus-update completion retains loaded reward metadata, fallback behavior and duplicate-callback protection.
- Java extension with compile-only declarations excluded from the shipped bundle.
- English documentation and semantic-release automation for subsequent releases.

Keep the default package name and the same local signing key to update an existing patched installation. Device startup and live bus updates require phone validation after the Java extension migration.
