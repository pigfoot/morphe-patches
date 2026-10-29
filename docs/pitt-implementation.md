# PiTT implementation contract

## Target and patch selection

Clean PiTT **12.0.0 / 40892**, `com.ihad.ptt`, original ARM64 APKS and minSdk 26.
Preserve density/language splits, package and display name. Reject unsupported or
modified targets, including v11 and reapplication. Root mount is not qualified.

Two independently selectable policies are maintained, with no extra parameters:

- **PiTT local sponsored features**: selected by default.
- **PiTT GmsCore Google sign-in**: opt-in; requires separately installed
  MicroG-RE 7.2.2 (`app.revanced.android.gms`). See the
  [adapter contract](pitt-gmscore-study.md).

The user reported successful v12 login and completed sufficient phone testing for
closeout. This does not establish every account/device, backend quota, cancellation
or token-expiry scenario. Build/static proofs and device observations are separate.
Temporary Google/Firebase diagnostics and added failure messages are not shipped.

## Local sponsored policy

PiTT shares a local sponsored flag between original ad suppression and IP-detail
quota gates. A getter or startup-only override misses direct readers and later
resets. Pin the five original writer/UI classes and inventory every field writer.
Redirect the six writes in five methods to one public static synthetic setter in
the existing state class. It accepts the original boolean and receiver, writes
true and returns; caller register values, branches and exception flow remain.

The no-purchased-subscription callback can show a misleading expired/payment-
failed/paused notice after this override. Replace only that exact toast invocation
with a no-op, retaining its callback/reset, pending-payment and unrelated errors.
Do not early-return receipt/network methods or fabricate subscription cache data.

The subscription UI can reflect the local state. This is not a purchase, valid
receipt, server entitlement, unlimited IP service or telemetry suppression.
Original Billing, its six-hour in-memory verification throttle, backend requests,
cache cleanup and network responses remain; the throttle is not evidence of a
fixed six-hour background job.

## Authentication and identity boundaries

GmsCore handles the existing Google ID-token request for the original audience.
The separate Firebase Auth SDK sends the original certificate metadata only in
its qualified HTTP header; see the [identity rationale](pitt-firebase-client-identity.md).
Keep real Google issuance, Firebase credential validation and PiTT account callbacks.
No fabricated token/account/purchase, global signature hook, replacement API key,
redirect URI, Firebase project or broad Google-service rewrite is included.
The original Google result and Firebase success/failure callbacks are unchanged.

## Validation and maintenance

- Preserve version/build, class fingerprints, all-writer inventory, missing-class,
  helper-collision and reapplication guards. Never relax them to accept a new app
  version; independently qualify pristine base and untouched merged inputs.
- Retain policy, branch/exception/register, expiry notice, GmsCore manifest/client
  and Firebase header tests. Removed diagnostic-only tests have no shipped owner.
- Apply the exact official-CI MPP to local-only, GmsCore-only, combined and full
  original APKS inputs. Runtime outputs must equal the corresponding qualified
  v6 selections with diagnostics unselected; never compare to a diagnostic build.
- Local-only changes six existing methods and adds one setter; GmsCore-only changes
  five existing methods and adds two client overrides; together they change eleven
  existing methods and add three. The original class set is unchanged.
- Preserve all other original method graphs/metadata. GmsCore manifest additions
  are one package query and two identity metadata entries only. Preserve original
  SDK, permissions, components, label and links.
- Resources/assets/native entries remain byte-identical. GmsCore resource-table
  recompilation must preserve complete package/type/config/entry semantics and
  resolved locale/string maps; do not claim raw-table byte identity.
- Exercise wrong package/version/build, missing/modified target, unexpected writer,
  pre-existing helper/manifest additions and reapplication. A wrong-package engine
  filter is zero patch application, not a hard error; distinguish it in reports.
- Run Java 21/SDK 36 official build/tests/catalog and release-tooling checks; use
  the [exact-head gate](ci-preflight.md). Keep generated catalog/versioning under
  automation. PR preparation is not authorization to merge or publish.

## Closeout scope

The earlier diagnostic patch, numeric Google failure instrumentation and Firebase
error formatter were investigation aids. They are removed together with their
exclusive tests and temporary diagnostic plan. Final feature code and meaningful
regression tests remain. The [sign-in survey](pitt-google-signin-study.md) is kept
as clearly labelled historical research, not as the current user workflow.
