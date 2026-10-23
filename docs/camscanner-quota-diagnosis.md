# Temporary lottery quota diagnosis

## V19 passive page-state comparison

V18 live output returned three in a successful separate query while the wheel
still displayed zero. The diagnostic intentionally does not synchronize Vue data;
this is not proof that the page's earlier request returned three or that the
failure is a particular auth, initialization, timing or parameter bug.

The current official component initializes its count to zero. Offline execution
of the actual view and public quota function reproduces both a failed earlier
query followed by a successful separate query, and a route credential parsed as
an array rather than a string. Either can leave the display at zero; neither is
established as the device cause. A normal page query returning three updates the
component to three.

V19 adds **passive** snapshots before and after the same single diagnostic read.
A bounded traversal of the production Vue root locates one component with the
measured lottery data/method shape. Only count/readiness/timezone-presence/modal
booleans, credential types/equality booleans, capped query-key occurrence counts
and bridge-ready boolean are reported. Credentials, usernames, URLs and arbitrary
objects are never output. Missing, ambiguous, cyclic or oversized trees are
classified conservatively. No component method is called; no UI field, route
parameter or server response is rewritten, and no refresh or extra query is added.

`UI_BEFORE` / `UI_AFTER` describe observed state, not a reconstructed history.
`TZ_EMPTY` or `READY_NO` alone does not prove why initialization failed.
Current labels are `CS-Q19:HOOK` / `CS-QUOTA-V19`.

## V18 exact public-module profile correction

V17 device output confirms the native hook, main JavaScript and legacy lottery
route execute, but the already-loaded `pc-api-DWt5-aHp.js` differs from the pinned
legacy build. `MODULE_NOT_LOADED` with zero quota reads is a diagnostic
compatibility failure, not proof of backend zero or authentication failure.

V18 retains the liveness stages and adds only this independently reviewed
URL/SHA256/export profile. The new module exports the original `queryLottery`
function as `i`, not the earlier named export. An exact loaded profile is selected
before a credential-free source fetch, hash verification and import; multiple
qualified legacy profiles in one document fail closed without fetching or reading.
Unknown build tags never become import URLs. The original read function, argument
scope, once-per-document guard, no-retry/no-write policy and normal modes remain.

The original public module was executed offline with synthetic cryptography and
transport boundaries: zero, three, missing and HTTP/business errors; exactly one
PAPI GET `/query_property` with `property_id=watchad_vip`, no write calls. This
checks code semantics, not live credentials or device quota. Screenshot credential
presence booleans still do not establish valid authentication.

Current markers are `CS-Q18:HOOK` and `CS-QUOTA-V18`.

## V17 liveness correction

V16 was selected explicitly on the phone but no diagnostic box appeared.
Its route matcher could silently wait and then disconnect without any report;
there was no independent evidence that the native hook or JavaScript had run.
The actual phone failure branch is still unknown; neither UI zero nor absent
output proves a backend quota/authentication result.

Only the existing explicit temporary mode adds these markers:
- A native `CS-Q17:HOOK` Toast classifies SCOPED, HOST_OUT_OF_SCOPE or NO_PAGE_URL.
  It never includes the actual URL or an identifier. Watch for it on page entry.
- A separate query-free JavaScript bootstrap creates `CS-QUOTA-V17` with
  `STAGE=JS_BOOTSTRAP`; it cannot overwrite an existing report.
- The main script immediately renders `STAGE=MAIN_ENTERED`, even while waiting
  for the route. An unrecognized route ends after30seconds with
  `ROUTE_NOT_RECOGNIZED_NO_READ`, not a silent disconnect.

The two JavaScript evaluations remain limited to the same official HTTPS hosts;
actual quota reads retain the original strict route/module/hash/once guards.
A Toast without a box narrows the investigation to evaluation/host/page execution;
a bootstrap-only box separates bootstrap execution from main-script execution.
These are liveness observations, not proof of a fixed phone or awarded reward.
No new helper class, public patch or default is added. Normal modes have neither
Toast nor diagnostic scripts. Both scripts execute on the existing UI callback;
all original WebView method bodies and parameter registers are preserved.

## Motivation and status

The Patcher2.0 input-fingerprint fix installs on the reported phone, but the
pre-draw exhausted dialog persists. The preload correction is not a demonstrated
fix for that symptom. Native entry code reads `day_count` from `/reward/big_lottery`;
the served legacy `/lottery/relottery` component reads `watchad_vip_chance` through
`query_property`. The separate modern integral route uses `/reward/big_lottery`.
The actual phone route and response are still unknown; do not equate the counters.

## Explicit temporary mode

Five public patch names and the block default remain. Under disable ads, select
**Skip lottery video + quota diagnosis (temporary)** (`inspect-lottery`) only for
pristine72701 online investigation. It retains the qualified skip/preload policy
and adds a pinned `WebViewFragment$IsWebViewClient.onPageFinished` prefix.
No new helper class, general Advanced/debug patch or recommended default is added.

The script runs only on the measured official hosts, identifies the lottery route
and requires its already-loaded, exact-SHA256-qualified official module. It calls
only that module's original read-only quota function once, with the original
page/session context. It never calls draw, claim, reward or task-completion APIs,
changes responses, rewrites counters, or retries the quota request. Unknown
modules/route/hash fail without a read. Do not press draw during this diagnosis.

- Earlier legacy module: `pc-api-BrpmLW-U.js`, SHA256
  `be6d3ff01592d1e952392801b2f5536cda89486f451cc90d1d060c219c23b636`,
  original `queryLottery` → PAPI `query_property` / `watchad_vip`.
- Current legacy module: `pc-api-DWt5-aHp.js`, SHA256
  `cb0dd1b74bc22ab7710971fdd2d78ee1d108bf6189f9eda74d0e80049f910cce`,
  original `queryLottery` exported as `i` → same read-only PAPI query.
- Modern module: `config-Dyz9RPJ4.js`, SHA256
  `29cb6e0058ef876d76b6dd60f611d54cc8cb62ab6a6a637578c1d34b5978c646`,
  original export `I` → UAPI GET `/reward/big_lottery`.

## Output and limits

The temporary `CS-QUOTA-V19` box reports route class, genuine count versus
null/missing/false/error, a bounded request count and credential-presence booleans.
It may report a strictly bounded public module build tag if a different version is loaded.
It never prints credentials, account/device identifiers, names, full URLs, response bodies or
exception messages. Presence is not evidence of valid authentication.

A passive XHR observer reports only numeric HTTP/business codes and the relevant
count-field classification. `_OBS` fields are observations, not guaranteed
request-correlated results; `OBSERVED_READS>1` explicitly exposes concurrency.
No XHR result/arguments are replaced, and its wrapper is restored after completion
or a10-second deadline when it still owns the property. Later SDK wrappers are
not overwritten; even if one retains the diagnostic wrapper, it adds no further
observers after cleanup. No capture is labeled NO_CAPTURE, not a fabricated HTTP
result. The original request is not aborted on timeout. Non-target page watching
is bounded30seconds and ends with an explicit no-read classification. One diagnostic read per document prevents finish-event
replay; returning later in the same SPA document does not trigger a second read.

## Validation before delivery

Execute the actual script with controlled module/XHR boundaries: real zero,
three, null, missing and false; both routes; HTTP/business/network errors;
unsupported source/hash/host and missing token; finish-event duplication; timeout;
concurrency; privacy canaries and original XHR return/wrapper restoration. Browser
cryptography is modeled as a boundary; public module digests/function semantics
were separately measured and inspected before qualification.

Assemble and execute the native prefix, checking exact script-string round trip,
host/null guards and untouched parameter registers. Apply supported engine2 full
APKM/combined and legacy cases, preserve all original method bodies outside the
three scoped prefixes, and reject a modified probe-client class. The diagnostic
must be removed or deselected after RCA. Neither CI nor a locally displayed count
proves the backend granted a reward or repaired phone behavior.
