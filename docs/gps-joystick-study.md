# GPS JoyStick 5.3.3 — static feasibility study

## Scope and provenance

Analysis only; no GPS patch or device/runtime acceptance is claimed. Candidate
source: `com.theappninjas.fakegpsjoystick`, version **5.3.3 (3030)**, minSdk23,
targetSdk36, ARM64 APK. The supplied third-party ANiK555 reference has the same
package/version/build but a different signing certificate. It is not a clean
baseline, this repository's output, or a source of credentials/native libraries.
Public indexed APKMirror metadata corroborates the source identity, but the
landing page is challenged; direct page/provenance verification remains partial.

## Premium already includes ad-free

The original upgrade controller (`sd/x.c`) maps the Pro monthly SKU to `PRO` and
the remove-ads monthly/yearly SKUs to `AD_FREE`. Its state application method
`sd/x.e(String)` maps none/ad-free/pro to levels1/2/3. It writes
`PRO_SUBSCRIPTION_ACTIVE` (`zd/f0.P0`) using the level3 predicate and writes
`REMOVE_ADS_SUBSCRIPTION_ACTIVE` (`zd/f0.E0`) for either paid level. The restored
state callback `g3/e.e(int)` follows the same rule; `k7/j.c(int)` is exactly the
level3 predicate.

Therefore the proposed installer-facing feature is **one Premium feature that
includes ad-free**, not a second redundant ad-removal option. App-open,
interstitial, banner, self-served and rewarded/Pro-intent gates are validation
coverage, not automatically separate user-facing patches. Identify all relevant
availability readers and restoration paths before implementation; do not modify a
generic R8 utility globally just because it is used by this flow.

Real Google Billing receipt validation (`ReceiptValidationService.validate`) is
separate from local feature availability. No purchase/validation request was sent;
local behavior must not be presented as a real server subscription.

## Maps is a separate capability

The reference changes the literal Google Maps API key, not merely a resource ID.
Only value hashes were compared; key values are not recorded here or adopted.
This is a verified map-related difference, **not proof that the key change alone
fixes maps**. Re-signing, key restrictions and any extra reference hooks require
independent qualification. Do not embed the reference author's key. A supported
Maps path must be investigated independently of Premium.

## Third-party reference is not a minimal patch

| Observation | Source | Third-party reference |
| --- | --- | --- |
| Classes / DEX | 28,062 / 4 | 28,075 / 10 |
| Same app namespace classes | 417 | 417 |
| Original ARM64 native payloads | 3 | Same three payloads, byte-identical |
| Additional packaging | — | ARMv7 variants plus ANiK555, dexprotector and pixel native libraries |

There are16 added and3 removed class names. The reference also adds six manifest
permissions, including privileged location/settings and package-install requests.
A permission declaration is not evidence that Android grants that privilege.
Do not copy the reference manifest/loader/native additions wholesale.

Its protected/obfuscated DEX triggers invalid-array-payload failures in the retained
SDK disassembler. Partial headers/native stubs are available, but an exact donor
method patch list has **not** been established. No third-party binary was executed.
Independent implementation should start from the readable candidate source.

## Shared capability fit and validation frame

- No expected Pairip license class exists in the source, so the current Pairip
  helper must not be selected automatically. The app-owned installer check `bf/a.b()` is called from the setup/overlay-denial
  flow (`bf/e0.invoke`, event `setup_overlay_denial_shown`), not from Pairip.
  It must be evaluated as Android permission/setup behavior, not blindly bypassed
  with the Java license helper; a spoofed installer string does not itself grant
  a system permission.
- Original certificate identity extraction is reusable only when an independently
  traced app adapter needs it. The Firebase header insertion helper is not a
  general Maps fix and must not be relabeled as one.
- Pinned Hoo-dles and Doom repository trees inspected for GPS/joystick/mocklocation
  path names had no dedicated matching paths. This narrow tree-name check is not
  a claim that no other author supports the app.
- Before changing code: qualify input signer/content pins and all local Pro/ad
  readers; design cold-start, restore/reset, offline and normal-usage tests plus
  unsupported/modified/reapplication guards. Premium must imply ad-free without
  forging purchases. Map loading/search/routing and signature/key restrictions
  have a separate success criterion; no key/hook transplantation.
- Preserve actual APK package identity, label, signing/update continuity and
  unrelated resources/native payloads. UI title wording is not package renaming.

Next implementation should be scoped from these findings. No implementation,
push, merge or release follows merely from this analysis document.
