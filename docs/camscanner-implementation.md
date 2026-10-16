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

The CamScanner patches are independently listed and disabled by default:

- **CamScanner local premium features**: `folderLimit` = 0 (original limit) or 5000.
- **CamScanner disable Firebase telemetry**: `scope` = `all` or `analytics`.
- **CamScanner disable app logging**: `localOutput` = true or false.
- **CamScanner disable ads**: `manifestCleanup` = true or false.
- **CamScanner offline mode**: includes ads suppression through a dependency; does not imply local features, Firebase suppression or scanner compatibility.
- **CamScanner scanner initialization compatibility**: separately opt-in, qualified ARM scanner library patches only.
- **CamScanner Word export diagnostics**: temporary diagnostic-only labels on three Word result-103 dialogs and recent numeric HTTP observations. It does not bypass limits or change conversion outcomes.

### No-ADB Word diagnostic v3 (experimental)

The first diagnostic covered only WordListPresenter. Other Word UI paths can
show the same quota confirmation dialog. V3 qualifies pristine fingerprints
for WordListPresenter, ImageToWordFragment, BaseJsonDocConverterFacade and
Json2WordCallable before mutation; all three dialog sites receive distinct
`CS-DIAG-V3` path labels, retaining the original result-103 condition and button.

One small Java helper keeps a bounded in-memory history of Word preparation and
numeric HTTP error-header observations. V3 also reads optional
`X-IS-Error-Msg` and `X-IS-Error-Code-Ext` from that same existing response.
Only fixed keyword-match hints (QUOTA/AUTH/SIGNATURE/DEVICE/UNCLASSIFIED/EMPTY)
and a bounded numeric extension code are retained; no arbitrary message text.
Hints are heuristic word matches, **not proof of the backend rejection reason**.
Missing headers and unavailable reflection are explicit; the body is never read
by diagnostics. V3 additionally observes only the nonempty/empty booleans for
the exact token and encrypted security-device field already used in the original
request. True means present, **not valid or entitled**. Neither value is retained.
It also observes the original parsed code header;
no request, quota, return value or response body is changed. No account, token,
URL, document content, arbitrary header text or on-disk diagnostic log is retained.
A null/nonnumeric observed code value is marked, not silently treated as zero. Events older
than 120 seconds are omitted. Recent events are explicitly process-local, **not
per-document correlated**: concurrent exports may appear together, and no recent
observation means UNKNOWN, not proof that a request was never sent. The main numeric code observation uses the exact header value already read by
the original code (including its original default); it does not distinguish a
missing main header from that default. The other fields above are separately
observed presence flags or sanitized hints.

Rebuild from clean supported input using the same prior options plus diagnostics.
Record one export attempt, then screenshot the failure dialog. Do not apply to an
already patched APK. This bundle is a diagnostic, not a restored Word/reward
feature or a verified phone-runtime result. Remove it after investigation.

Validation frame: build/tests; real diagnostics-only and combined application;
exactly one injected helper, exactly four qualified modified app classes; preserve
original instruction/register/branch/handler graphs, permissions, assets and native
binaries (except independently selected scanner compatibility). Test all three UI
sites, bounded/concurrent history, numeric/missing/nonnumeric values, diagnostic-off
unchanged, modified/unsupported rejection and unchanged result0/other errors.

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

## Donor comparison: what paid-feature and native changes establish

The supplied ad-support sample is a reference, not proof of complete paid-feature coverage.
Its added `bin.ghost` bootstrap changes the Application inheritance path and loads an
additional `libyrf.so`. In the inspected ARM64 JNI path, a conditional branch writes true
to the injected static boolean `LogUtils.〇〇878`. Two AccountPreference predicates and
one SyncUtil predicate consume this flag as a fallback instead of false. Despite its
placement in LogUtils, this is an entitlement/status flag, not the logging switch.
Another account predicate returns true and the folder-cap getter returns 5000.
This demonstrates local gate manipulation, not a genuine subscription or server entitlement.

The added payload also contains signature-spoofing and donor validation/dialog/update
machinery; its entire behavior has not been qualified. This project does not copy
`libyrf.so` or the `bin.ghost` machinery. Local-feature patches change qualified DEX
predicates directly. The project also changes the local long-valued status getter,
which the donor leaves preference-backed; do not claim identical entitlement semantics.

Separately, the donor modifies the existing `libscannercs.so` in two ABIs. The observed
change is an immediate zero return at `verifyKeyV2`: eight bytes in ARM64 and four bytes
in armeabi Thumb. No crop-algorithm change was found in those scanner-library diffs.
Re-signed scanner initialization compatibility is a hypothesis requiring runtime evidence,
not an established all-device crop fix. The optional scanner compatibility patch reproduces
these qualified entry changes, independently of local feature selection.

The three combinations are reference-inspired reimplementations, not equivalent donor copies:

- Local feature predicates target similar local checks, with different implementation paths.
- Firebase/logging suppression adds persisted-override, re-enable and cache-replay defenses.
- Clean removes reviewed ad-only manifest components, retaining non-ad login/Billing/WorkManager
  entries removed by the donor. Donor ad-support and clean have identical DEX/native/assets.
- Offline removes every INTERNET permission; the donor retains INTERNET and instead changes
  connectivity returns, URLs and related literals. It is not the same network policy.
- Repeat ad rewards, every paid feature, server credits and cloud quotas are not certified.

## Next step: device qualification before merge/release

1. Back up app documents first. Re-signed output may not update an existing official install;
   updates require a compatible signing key. Do not uninstall a document-bearing installation
   without a verified backup.
2. Use the qualified pristine complete APKM. Start with local features, Firebase scope `all`
   and app logging; leave ads, offline and scanner compatibility unselected to isolate failures.
   Configure folderLimit explicitly if testing the 5000 option.
3. Test launch, scan/import, crop, OCR and PDF export. List each paid feature of interest and
   record whether it actually works; a status boolean is not a substitute for these results.
4. If scanner initialization fails, compare the optional compatibility patch with the same
   input, options and signing setup. Record device/ABI and reproduction steps; avoid assuming
   a crop failure is caused by signing.
5. After basic flows work, qualify ads/clean and offline separately: asynchronous ad fallback,
   login/share integrations, foreground/background requests and external-app/service behavior.
   Capture network/log evidence before claiming telemetry or connectivity suppression.
6. Fix reproducible failures, review the draft PR, then authorize merge/release. This document
   records a test plan, not completed device qualification.

## Local-premium label clarification

Motivation: the previous "local features" label obscured the intended local paid-feature
scope. Rename the public patch to **CamScanner local premium features** without changing
bytecode targets, options, compatibility, dependencies or disabled-by-default behavior.
Success: the rebuilt bundle exposes the new name; the prior name is absent. Existing
selection/dependency/option tests and official compilation must pass. Historical validated
bundles may still expose the previous label. This is not a reward-playback fix.

## Open runtime issue: task-center lottery

A reported symptom is no visible reaction when requesting a rewarded ad from Task Center,
Daily Tasks, Lottery, using local premium/Firebase/logging/scanner compatibility without
ads suppression or offline mode. A cloud-capacity concern was withdrawn; do not change
quota handling in response. The root cause of the lottery symptom is not established.

Static entry tracing finds WebAction's `watchad_lottery` action constructs WatchAdControl
with LeftRewardVideoManager; it is distinct from AdRewardedManager's document-function
reward paths. No fake ad-completion or fabricated task reward should be introduced.
The next evidence is exact click/loading behavior and a same-device donor/original control,
then a minimal patch-selection A/B. Build-only validation does not establish normal
availability of an ad or successful task-center callbacks.

### Minimal-selection runtime control

The lottery no-response and Word-limit symptoms were also reported with only scanner
initialization compatibility and local premium selected, without the Firebase or logging
patches. Thus neither suppression patch alone explains the report. This is user-reported
runtime evidence, not an independently reproduced fix or proof of the remaining root cause.
The previously supplied ad-support donor can play the lottery ad and convert Word according
to the control report; completed daily-task rewards remain a separate unverified outcome.

Next inspect the exact device-generated APK: applied method bodies, manifest/DEX/module
inventory and entry-point selection, against the independently validated output and donor.
Do not keep toggling unrelated options or assume source-level success guarantees identical
Manager/device output. The actual generated APK and exact Word entrance are still needed;
no corrective bytecode change is justified yet.

## Temporary startup warning caller diagnostics

`CamScanner startup warning diagnostics` is independent and opt-in. It annotates
only the qualified local piracy warning with `CS-BOOT-TRACE` and up to four
app-owned class/method names from the live call stack. It does not suppress the
warning, change checks, spoof signatures, or imply that startup is repaired.
Other messages retain the same CharSequence object. No filenames, line numbers,
arguments, account identifiers, disk logs, or network requests are emitted.
Unsupported or previously instrumented dialog-builder definitions are rejected.

Both startup and Word diagnostics share a single extension dependency. Selecting
both must still inject only `app.pigfoot.diagnostics.WordTrace`. A successful
build/static APK comparison is not a substitute for observing the phone's
actual warning caller.

## Shared local sideload startup gate

All non-diagnostic CamScanner features depend on `CamScanner sideload startup
compatibility`. The local Java piracy-warning gate must not depend on selecting
the separate optional native scanner initialization feature. The shared gate
replaces the entire qualified `PirateAppControl.O8(Context, DialogDismissListener)`
body with a constant-false return, preserving its signature, access flags,
annotations, and register count. No old dialog instructions or exception
handlers remain in that method. The separate same-message Toast entry,
`PirateAppControl.Oo08(Context)`, is replaced with a single void return,
also preserving its method contract and register count. Both entries are
validated before either replacement is applied. The shared predicate and
other five methods in this class remain unchanged. Unrelated Toasts and
other piracy/risk checks, signature APIs,
account status and cloud quotas are not changed by this startup patch.

Diagnostics remain independently selectable to observe the baseline. Native
scanner edits retain their existing exact hash/ABI guards and remain optional.
A phone acceptance test must cover first open, close/reopen, and delayed reopen;
a passed build or a constant-return DEX entry is not a runtime acceptance result.
Acceptance requires absence of both the modal warning and the bottom Toast.

## Optional email-login error diagnostics

### Optional production account-service fallback compatibility

`CamScanner account API fallback compatibility` is a separate, default-off
experimental candidate for the exact qualified version and pristine CsHosts
fingerprint. A bounded synthetic reserved-domain account query observed HTTP500
without a structured error header at the production backup endpoint, but HTTP406
with application code201 at the catalog primary endpoint. LITE/FULL controls
gave the same respective outcomes. These transport probes are not equivalent
to phone login and do not establish that every backup request fails.

The production fallback literals in `CsHosts.o0ooO` (login UAPI) and
`CsHosts.〇00〇8` (main UAPI) change from
`https://api-cs-bak.intsig.net/user/cs` to the existing catalog endpoint
`https://api-cs.intsig.net/user/cs`. The first candidate preserved nonblank
configured UAPI values, and the phone continued to report the backup route.
The revised candidate also normalizes that exact backup base when returned by
configuration. Every other configured value, including regional hosts,
lookalikes, trailing-slash variants and sandbox, remains unchanged. No retry,
response-code rewrite, authentication bypass, credential storage, signature hook,
native edit or membership change is added.

Both configured-return instructions are replaced by the first instruction of
an exact-string guard, preserving incoming branch targets. Original registers,
neighboring method graphs, sandbox paths and error behavior are retained.
Validation must independently execute both getters with null/blank, exact old,
primary, sandbox, regional, lookalike and trailing-slash configurations with both
sandbox states, compare standalone class/native/asset inventories, and exercise
clean-input guards and compatibility with existing optional diagnostics.
Device acceptance still requires genuine email login and reopen;
successful build or a synthetic unregistered-account response is not that result.

### Error observations

`CamScanner email login diagnostics` appends `CS-LOGIN-DIAG-V6 code=<integer>`
to the existing email/password error text. This is diagnostic instrumentation,
not a login repair. Authentication, requests, original exceptions and error
branches are preserved. It does not target Google login, phone login or every
authentication UI.

Three original classes are fingerprint-guarded before mutation. The qualified
TianShuAPI HTTP-500 exception branch gains one observation before its original
throw. Only the existing response's numeric error-header category and message
presence are observed; no body or credentials are read. V4 additionally parses
the URL prefix already present in this exact original exception formatter.
It retains only `/query`, `/sync/query` or `/user/cs/query` (other paths become
`OTHER`), known account primary/backup production/sandbox and legacy sync host
classifications (all other hosts become `OTHER`), and the exact qualified
`CamScanner_AD_1` FULL/LITE client and version with optional `.Debug` suffix.
Other client values are redacted. Userinfo, fragments, non-HTTPS URLs,
unexpected ports, duplicate client parameters, malformed and oversize inputs
produce unavailable output. Raw URLs and all other query values are discarded.
Host classification is not evidence of which configuration source selected it.
V5 corrects V4's bare-URL fixture assumption: the original exception overrides
`getMessage()` with `TianshuException (500, <formatter>)`. Only this exact wrapper
is unwrapped. End-to-end regression tests reproduce the original method's
format, and unavailable output includes fixed reason categories, never raw input.
V6 corrects the earlier incomplete route derivation: the account-existence
selector `1` is transformed by `O8O〇88oO0` into network API type `29` before
`UserInfo.getAPI`. Its switch selects `CsHosts.o0ooO` / `ApiCenterInfo.UAPI`,
whose production fallback is `https://api-cs-bak.intsig.net/user/cs`, not SAPI.
Appending `/query` produces `/user/cs/query`. End-to-end fixtures cover this
complete chain's public default route. Unknown paths are redacted independently
and no longer discard otherwise safe host/client observations. This is not
proof of a runtime destination or the cause of a server error.
Missing, empty, numeric zero, other bounded numeric values, invalid values and
unavailable reflection are distinct. The exact exception is a weak identity key;
HTTP status is not inferred solely from a message-format marker.

The view-model handler gains an observation of its exact exception and a bind
before each of its six original ErrorMsg postValue calls. A synchronous
thread-local sanitized handoff binds metadata to that exact ErrorMsg object.
The UI consumes it only by object identity and matching numeric code, never by
code/time proximity or equals(). UI scheduling can cross threads. Cache size is
16 weak keys with a 30-second monotonic expiry; overflow, mismatches and missing
links produce explicit unavailable output rather than borrowed metadata.

Fixed stack-caller labels distinguish the initial account-existence query from
the password-login precheck where those exact frames are present. Unknown
callers remain unknown. Existing bounded path/message-layout hints, numeric
codes and up to two app-only source names remain available; keywords are not
proof of rejection cause. URLs, raw exceptions/messages, account values,
tokens, device identifiers and response bodies are not retained or emitted.

All original instructions, register counts, branches, handlers and method
metadata must remain after removing documented instrumentation. The shared
extension remains one helper class. This option is independently selectable,
does not change signature APIs, and does not fix login or Word by itself.

### Email diagnostic V7: business-response provenance

V6 only captured transport metadata at the qualified HTTP-500 throw. A subsequent
application-201 exception consequently had no matched transport metadata. V7 adds
one passive call immediately after the original generic HTTP-406 business-error
constructor. The exact pinned original class and constructor/register context are
required; the helper only records application code201. Existing authentication,
request arguments, routing, exception and UI result branches remain unchanged.

The exact exception-to-event identity handoff now exposes HTTP406, bounded error
header state, allowlisted account host/path/client and whether `emailEncrypt` is
missing, empty or present. No account value, ciphertext, hash, length, credential,
raw URL, response body or error message is emitted. Existing HTTP500 observations
remain available. Unmatched records remain explicitly unavailable. The display
marker is V7; the account fallback compatibility option remains independent.

This is an evidence correction, not a functional login repair. Successful build,
unit tests and unchanged original method graphs do not demonstrate phone login.

### Native request signing and official service discovery

The previous account fallback option is withdrawn: the catalog explicitly
separates UAPI from UAPI_CN, so substituting the latter's default is not a
region-equivalent fallback. Stale forced selections now fail with an explanation.

The independent, default-off account service signing compatibility option pins
both original `libnative-encrypt.so` binaries and replaces only the caller's
skip-computation branch in `NativeEncrypt.getSignParameter`: one ARM64 instruction
and one Thumb instruction. A local signer mismatch otherwise returns an empty
signature before the existing native salt/MD5 computation. The original package
certificate passes the gate; re-signed outputs do not. The checkSignature function
itself, all other callers, cryptographic routines and service request code remain
unchanged. No global package-manager hook, forced hostname, response rewriting,
password validation bypass or server entitlement change is introduced.

Isolated ARM64 instruction execution with simulated JNI/libc/MD5 boundaries checks
both API-type flags and empty/nonempty inputs. The changed caller matches the
original accepted-signer computation, while the unmodified re-signed case returns
empty. A bounded synthetic signed API Center lookup returns the official US UAPI
separately from UAPI_CN. These checks are not proof of phone authentication.
V8 diagnostics additionally classify that official US account service; all
identity values and signing material remain redacted. Retain the original API
Center cache/discovery behavior. Do not select the withdrawn fallback or offline
mode when testing legitimate online login.
