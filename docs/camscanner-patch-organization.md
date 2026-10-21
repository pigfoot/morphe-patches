# CamScanner patch organization proposal

**Status: documentation/design only.** The implementation snapshot reviewed here is
`bf369ef`. This document does not change a patch, option, generated patch list,
MPP, APK or release. The proposed organization is not available in the current bundle.
It preserves the official Gradle layout and leaves RailsGo unchanged.

## Motivation and success criteria

The current bundle exposes twelve named CamScanner patches accumulated during
feature work and diagnosis. This mixes product choices, required repack support,
withdrawn experiments and temporary observation tools. In particular:

- Account signing compatibility is already REQUIRED for supported standard/Shizuku
  installs, yet startup compatibility remains a separately named dependency.
- Signing compatibility depends on the complete email-login diagnostic patch.
  Disabling observation is therefore not independent of preserving working login.
- `manifestCleanup=false` on **disable ads** still suppresses ad entry points.
  It only retains manifest components; it does not mean ads are enabled or rewarded
  ads are preserved. This is an implementation detail, not a useful everyday choice.
- Firebase `scope`, logging `localOutput` and three diagnostic entries expose
  low-level mechanisms without explaining a coherent user intent.
- A withdrawn account-route patch remains listed as an unavailable tombstone.

Success means users choose outcomes, cannot accidentally omit required repack
compatibility, and never have to enable diagnostics to obtain a functional fix.
Independent feature intents remain independent; the old donor variant names do
not become three monolithic presets. Working request signing/discovery behavior,
original identity/assets, input guards and honest server responses must survive.

## Proposed normal selection surface

One locked compatibility entry and four optional feature choices:

| Entry | Selection | Contract |
| --- | --- | --- |
| CamScanner compatibility | Required, no feature options | Qualified sideload startup handling, native request-sign computation and narrowly scoped API Center cache recovery. |
| CamScanner local features | Optional, off by default | Existing local status behavior only. Keep `folderLimit` original by default; 5000 remains an explicit advanced choice, not unlimited folders. |
| CamScanner reduce telemetry and app logging | Optional, off by default | Combine the current all-Firebase policy and qualified app-network/cache/local-log suppression. No everyday scope or inverted output bool. |
| CamScanner disable ads | Optional, off by default | One complete qualified ad-suppression policy, including reviewed manifest cleanup; no nested bool. Rewarded ads are not preserved by this policy. |
| CamScanner offline mode | Optional, off by default | Depends on disable ads and removes every app INTERNET declaration. Does not imply local features or privacy/logging suppression. |

**Advanced, separate:** scanner initialization compatibility remains optional and
experimental. It is not known to be universally necessary and must not become an
unconditional native edit just to shorten the list.

The compatibility entry is not another toggle users must discover. Manager should
resolve it as REQUIRED and display it locked, if displayed at all. Its component
patches are internal/unnamed. Do not assume an unnamed dependency can self-select:
if no optional feature is selected, the required compatibility root must still be
included by the supported installer. Verify the actual Manager and CLI behavior.

No named patch or product description should call this a generic “spoof”: the
current native fix preserves original request-sign computation; it does not replace
PackageManager certificates or globally patch signature checks. An Instagram trust
wrapper or opaque donor bootstrap is not part of this reorganization.

## Current-to-proposed mapping

| Current named patch | Proposed disposition |
| --- | --- |
| sideload startup compatibility | Internal component of compatibility root. |
| account service signing compatibility | Internal component of the same root; preserve qualified native edits and exact cache predicates. |
| local premium features | Keep independently selectable; clarify local/server boundary in name and description. |
| disable Firebase telemetry | Merge into reduce telemetry and app logging. Keep unrelated ML Kit, FCM and Remote Config functionality. |
| disable app logging | Merge into that same privacy choice; local-output policy becomes explicit documented behavior, not a bool. |
| disable ads | Keep independently selectable; remove consumer `manifestCleanup` option. |
| offline mode | Keep independently selectable, with ads/core dependencies only. |
| scanner initialization compatibility | Keep advanced/experimental; do not force into core. |
| startup warning diagnostics | Remove from normal feature catalog; maintainer-only diagnostics. |
| email login diagnostics | Remove functional dependency and normal listing; maintainer-only diagnostics. |
| Word export diagnostics | Remove from normal feature catalog; maintainer-only diagnostics. |
| account API fallback compatibility (withdrawn) | No functional replacement. Remove normal listing; preserve actionable stale-selection rejection during migration. |

Keep implementation modules and tests separate where useful. A smaller selection
menu does not require one giant source file or merging unrelated mutation logic.

## Compatibility core: separate policy from observation first

Before hiding or deleting any diagnostic entry:

1. Extract the exact obsolete/error-only API Center cache policy from the shared
   `WordTrace` helper into a small functional helper with no diagnostic UI, event
   history, credential capture, endpoint substitution or extra requests.
2. Preserve both original cache lookup hooks and existing fetch/persist fallback.
   Do not broaden invalidation or delete user preferences.
3. Preserve the exact native request-signing edits, ABI/hash/byte guards and the
   qualified startup dialog/Toast behavior. Do not replace global checkSignature.
4. Make input validation run once, before any mutation of shared classes. Remove
   the current reliance on login diagnostics to validate pristine definitions.
5. Attach all normal features to one required core. Confirm selection is idempotent:
   explicit plus transitive core selection never applies the same edit twice.
6. Only then move passive login/startup/Word instrumentation behind a separate
   maintainer diagnostic profile/bundle, not three consumer feature choices.

Required signing remains harmlessly present in the offline composition; it does
not grant INTERNET or perform authentication. Root mount remains unavailable until
independently qualified. Exclusive CLI diagnostic controls may omit core deliberately,
but must be documented as isolation tools rather than recommended installations.

## Ads: remove the ambiguous bool, not the safety contract

### Current meaning

| Current selection | Actual behavior |
| --- | --- |
| disable ads not selected | This feature applies no ad suppression or cleanup. |
| selected, manifestCleanup=true | Suppress qualified ad entry points and remove reviewed ad-only manifest components. |
| selected, manifestCleanup=false | Still suppress ad entry points, but keep those manifest components. |

The proposed public choice has only selected/unselected states. Reviewed cleanup
is an internal part of the selected policy. A retained-manifest A/B is useful only
in maintainer experiments, not as a misleading nested “disable” choice. If a removed
component causes a real non-ad regression, fix the allowlist/dependency model; do
not hide the defect behind an unexplained consumer bool.

The current target list and cleanup include rewarded-ad paths. Do not rename this
policy “remove intrusive ads” or promise watch-to-reward still works. A separate
“keep voluntary rewarded ads” mode requires its own verified load/show, genuine
completion/cancel/failure and server-grant behavior before being offered. Never
synthesize completion, repeat rewards, credits or successful server responses.

Preserve non-ad login/share, Billing, WorkManager and retained modules/assets.
No-fill, cancellation, fallback UI and missing-provider paths must terminate
without crashes, endless spinners or false success. Returning early from SDK
methods alone is not evidence these runtime contracts pass.

## Privacy policy and intentional trade-off

For the normal menu, prefer one opt-in policy with a fixed, clearly described
scope over several switches. It combines existing qualified controls; it does not
assert all telemetry, native reports or third-party logging are eliminated. It does
not strip unrelated Firebase components or disable the whole network.

The old Analytics-only and network-logs-only combinations are intentionally not
normal-menu choices in this proposal. Keep their reproducible A/B controls in
maintainer tooling if needed. Do not silently convert saved narrower choices into
the new broader policy. Source controls remain modular for regression isolation.

## Diagnostics, withdrawn code and migration

- Release installations should retain ordinary app messages, not CS-DIAG caller
  traces or error overlays. Preserve historical diagnostic artifacts as controls.
- A maintainer diagnostic profile can enable startup/login/Word observations as
  one explicit investigation capability; it must not add a functional dependency.
- Keep compatibility adapters only for safe equivalents. Old startup/signing
  selections can normalize to one core in a versioned migration layer if the
  patcher actually supports it; otherwise reject with a concrete replacement.
- Firebase/logging partial policies and `manifestCleanup=false` cannot silently
  map to stronger new behavior. Require explicit reselection with a clear error.
- Withdrawn forced-route selection must fail closed, never redirect to UAPI_CN.
  If old names cannot be rejected before discovery, retain an unavailable legacy
  tombstone temporarily in an advanced migration surface rather than silently
  dropping CLI selections. Validate the patcher limitation before promising removal.
- Scanner opt-in, root-mount rejection and modified-input rejection remain.
- Do not manually edit generated patch lists, Manager metadata or version numbers.
  Regenerate through the official build only after source implementation.

## Qualification before implementation can ship

| Case | Required evidence |
| --- | --- |
| Core-only standard/Shizuku | Core resolves as required with no optional features; exact startup/native/cache edits; original messages without diagnostic overlays. |
| Each feature alone | Core included once; unrelated premium/privacy/ads/offline/native-scanner policies remain absent. |
| Offline | All duplicate INTERNET declarations removed; ads/core included; platform network state not falsified; no forced auth/host/retry. |
| Cache known failing and nearby cases | Exact obsolete/error-only envelopes take original miss path; valid regional/mixed/unknown/malformed configs preserve prior handling. |
| Input/reapply/ABI guards | Original targets pass; modified/donor/wrong-version/unknown ABI/repeated edits reject without output. |
| Ads selected/unselected | Deterministic entry/manifest changes and preserved non-ad components; actual completion/cancel/no-fill/failure/fallback device behavior checked. |
| Privacy and migration | Fixed documented scope, unrelated Firebase preserved; old narrower configs rejected or explicitly reselected, not broadened silently. |
| Maintainer diagnostics | Core works without diagnostic UI; optional observations do not alter requests, authentication, quota or result codes. |
| Combined features | Shared validation/edit/helper deduplication, method/branch/register preservation, asset/module/class inventory and exact native changes. |
| Scanner advanced | No scanner edit in core; explicit scanner selection retains original ABI/hash guards. |
| RailsGo regression | Existing dependency/rename/reward/helper contracts unchanged. |

Use Java21/SDK36 and the official build/patch-list generation, then pristine-input
APK qualification and guarded negative cases. Do not equate unit/static validation
with phone startup, login persistence, ad behavior or zero telemetry.

## Current evidence and implementation boundary

The latest implemented signing/cache behavior has reported email-login success.
The current source snapshot passed91 CI tests and two diagnostic/combined APK
qualifications with two modified-input rejection cases. These results validate
the pre-reorganization code, not the proposed composition.

The examined Word route depends on server conversion; current tests do not show
a working cloud Word capability. Its phase is closed unresolved, not a new feature
or a reason to keep Word diagnostics in every normal install. No server-side
entitlement, quota, repeat-reward or universal scanner-fix promise is added here.

Implementation order: extract functional policy from diagnostics, introduce the
required core, reshape optional privacy/ads choices, add migration guards, then
regenerate metadata and run the full qualification matrix. This PR update only
records the design and corrects documentation; it ships no reorganized patch.
