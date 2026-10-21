> V12 update: the scanner initialization choice is retired, with its implementation
> archived outside compiled sources. The public catalog has five entries. Neither
> trust nor any other feature silently applies scanner edits. The previous V11
> qualification below remains historical evidence, not V12 acceptance.

# CamScanner patch organization and qualification

**Status: experimental candidate qualified for phone testing; not phone-accepted.**

The source now implements the six-entry surface below, with no shipped diagnostic
patches. It retains the existing native signing/cache recovery strategy, not a
certificate-spoof replacement. Do not treat a build as phone acceptance.

## Candidate recommendation settings

- **Required / default=true:** CamScanner restore trust on re-signed builds.
- **Recommended / default=true:** CamScanner local premium features; CamScanner
  reduce telemetry and app logging. These remain individually deselectable.
- **Not recommended / default=false:** CamScanner disable ads; CamScanner offline
  mode; CamScanner scanner initialization compatibility.
- Folder limit defaults to original0, not5000. Ads and INTERNET remain in the
  recommended online/ad-support composition. Mount stays unavailable.

Recommended means the patch metadata default and standard/Shizuku ENABLED
availability; only trust is REQUIRED. This is an experimental test recommendation,
not a guarantee of all local premium, privacy, reward or scanner behavior.

## Motivation and success criteria

The previous bundle exposed twelve named CamScanner patches accumulated during
feature work and diagnosis. That mixed product choices, required repack support,
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

## Candidate normal selection surface

One locked startup/login fix and four optional feature choices:

| Entry | Selection | Contract |
| --- | --- | --- |
| CamScanner restore trust on re-signed builds | Required, no feature options | Qualified sideload startup handling, native request-sign computation and narrowly scoped API Center cache recovery. |
| CamScanner local premium features | Optional, recommended | Existing local status behavior only. Keep `folderLimit` original by default; 5000 remains an explicit advanced choice, not unlimited folders. |
| CamScanner reduce telemetry and app logging | Optional, recommended | Combine the current all-Firebase policy and qualified app-network/cache/local-log suppression. No everyday scope or inverted output bool. |
| CamScanner disable ads | Optional, off by default | One complete qualified ad-suppression policy, including reviewed manifest cleanup; no nested bool. Rewarded ads are not preserved by this policy. |
| CamScanner offline mode | Optional, off by default | Depends on disable ads and removes every app INTERNET declaration. Does not imply local features or privacy/logging suppression. |

**Retired:** scanner initialization compatibility is not in the shipped catalog.
Its necessity was not demonstrated; archive the experiment without forcing its
native edits into the required trust root.

The required entry uses the outcome-based display name **CamScanner restore trust on re-signed builds**, not the abstract name “compatibility”. Its scope is the exact
qualified repack paths, not every possible startup/authentication problem.
**Local premium features** intentionally describes local subscription-gated
features and does not imply a server subscription, cloud credit or Word entitlement.

The startup/login fix is not another toggle users must discover. Manager should
resolve it as REQUIRED and display it locked, if displayed at all. Its component
patches are internal/unnamed. Do not assume an unnamed dependency can self-select:
if no optional feature is selected, the required compatibility root must still be
included by the supported installer. Verify the actual Manager and CLI behavior.

The trust name describes its purpose, not a generic certificate “spoof”: the
current native fix preserves original request-sign computation; it does not replace
PackageManager certificates or globally patch signature checks. An Instagram trust
wrapper or opaque donor bootstrap is not part of this reorganization.

## Previous-to-candidate mapping

| Current named patch | Proposed disposition |
| --- | --- |
| sideload startup compatibility | Internal component of compatibility root. |
| account service signing compatibility | Internal component of the same root; preserve qualified native edits and exact cache predicates. |
| local premium features | Keep independently selectable; clarify local/server boundary in name and description. |
| disable Firebase telemetry | Merge into reduce telemetry and app logging. Keep unrelated ML Kit, FCM and Remote Config functionality. |
| disable app logging | Merge into that same privacy choice; local-output policy becomes explicit documented behavior, not a bool. |
| disable ads | Keep independently selectable; remove consumer `manifestCleanup` option. |
| offline mode | Keep independently selectable, with ads/core dependencies only. |
| scanner initialization compatibility | Retire from compiled sources/catalog; archive only, never force into core. |
| startup warning diagnostics | Remove from normal feature catalog; retire from shipped catalogs, retain source/tests. |
| email login diagnostics | Remove functional dependency and normal listing; retire from shipped catalogs, retain source/tests. |
| Word export diagnostics | Remove from normal feature catalog; retire from shipped catalogs, retain source/tests. |
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
6. Only then remove passive login/startup/Word instrumentation from the shipped
   bundle. There is no Debug or Advanced debugging entry, switch or overlay.
   Retain source/tests and historical artifacts for a future concrete investigation;
   do not create a separate diagnostic profile/bundle as a product deliverable.

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
policy “remove intrusive ads” or promise watch-to-reward still works. This means
watch-to-reward opportunities stop working, not that reward/task buttons are
automatically hidden or existing server-awarded balances are erased. The product
description must explicitly say it includes rewarded ads. A separate
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
- Do not ship debugging in an Advanced menu either. Source/tests can be retained
  without exporting named patches, options or injected diagnostic extension code.
  A future real issue may justify an isolated temporary investigation build; that
  is not a planned user-facing feature or current deliverable.
- Keep compatibility adapters only for safe equivalents. Old startup/signing
  selections can normalize to one core in a versioned migration layer if the
  patcher actually supports it; otherwise reject with a concrete replacement.
- Firebase/logging partial policies and `manifestCleanup=false` cannot silently
  map to stronger new behavior. Require explicit reselection with a clear error.
- No forced route remains. Old patch names are unsupported, not automatically
  migrated. Desktop1.18.1 was observed to ignore an unknown removed name in
  exclusive mode, apply zero patches, emit an APK and return exit0. A bundle cannot
  claim this is fail-closed name validation. Do not use old profiles or treat CLI
  exit0 alone as selection success; reload the new MPP and reset Recommended, or
  use exactly the new CLI names and inspect the applied list. This limitation
  remains documented rather than re-exposing withdrawn/debug choices.
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
| No shipped debugging | Neither normal nor Advanced lists export diagnostics; core works with no diagnostic UI/helper history. Retained tests cover any later isolated instrumentation without altering auth/quota/results. |
| Combined features | Shared validation/edit/helper deduplication, method/branch/register preservation, asset/module/class inventory and exact native changes. |
| Scanner advanced | No scanner edit in core; explicit scanner selection retains original ABI/hash guards. |
| RailsGo regression | Existing dependency/rename/reward/helper contracts unchanged. |

Use Java21/SDK36 and the official build/patch-list generation, then pristine-input
APK qualification and guarded negative cases. Do not equate unit/static validation
with phone startup, login persistence, ad behavior or zero telemetry.

## How the online ad-support composition differs from a donor APK

To retain ads, choose compatibility plus the desired local/privacy features,
leave disable ads/offline unselected, and opt into scanner support only when
needed. “Ad-support” is a composition, not another bundled preset to select.

- Same intended outcome for ordinary login, with reported success, but different
  mechanisms: donor Application/bin.ghost/libyrf signature spoofing versus the
  narrow native request-signing/cache compatibility repair. Not full equivalence.
- Modular source-selected local/privacy behavior rather than an opaque fixed APK.
  No donor credit/update/validation bootstrap is copied. Whole donor runtime side
  effects and telemetry silence have not been completely reconstructed.
- Build from pristine full APKM and preserve original modules/OCR assets, rather
  than using the modified donor as a trusted baseline.
- Keeping ads does not prove rewarded callbacks or server grants work; original
  task/reward behavior still requires scoped qualification.
- Server-backed Word has no demonstrated working output in either the current
  candidate or the reported ad-support baseline. This is not a local entitlement
  promise, nor proof that both fail for the same backend reason.

## Current evidence and implementation boundary

Behavior source `c3d2ec8` passed official Verify39611793715 and Release39611787533:
91 tests, zero failures/errors/skips. The extracted cache policy also passed six
local Java tests. The exact CI MPP was applied to pristine merged input in five
cases: trust-only; real default/recommended selection; ads-only; offline-only;
scanner-only (each includes its dependencies). Modified/reapply and forced
wrong-version input guards rejected without output.

All five outputs preserve original identity/modules and contain only the new
stateless helper; diagnostic markers and the four original Word-class mutations
are absent. Login's332 original methods preserve graphs/registers/handlers except
two qualified cache-hook insertions; native signing stays at4+2bytes. Eleven
local/privacy classes canonically match the earlier qualified full policy.

These APK/static results do not establish new phone acceptance. Earlier email-login
success applies to the preceding signing/cache implementation; reopen persistence,
actual ad callbacks/grants, scanner and all telemetry boundaries remain distinct
manual qualification items.

The examined Word route depends on server conversion; current tests do not show
a working cloud Word capability. Its phase is closed unresolved, not a new feature
or a reason to keep Word diagnostics in every normal install. No server-side
entitlement, quota, repeat-reward or universal scanner-fix promise is added here.

The candidate extracts functional policy from diagnostics, introduces the
required core and reshapes optional policies. Official metadata generation and
qualification must use the actual final artifact. Previous implementation evidence
is historical; the new output must be qualified separately. Old selection profiles
are not a supported silent migration: rebuild from pristine input and select the
new recommendation set. Do not infer that removed option keys are safely mapped.

Historical diagnostic patch source is archived as text under
[diagnostic source archive](historical/camscanner-diagnostics/). It is not compiled
or exposed. The production extension contains only the stateless LoginApiPolicy;
WordTrace is retained only in test sources, with old helper tests retained.
