# CamScanner lottery video skip experiment

## Motivation and status

The existing disable-ads policy intentionally blocks rewarded video entry points.
A failed video-load dialog is consistent with that policy, not a login regression.
A new explicit request is to skip the video while still allowing the original
lottery/task reward flow. A local completion is not proof of a server credit.
This is a CI/APK-qualified experiment for phone testing; real reward receipt is
not accepted: a phone report shows an exhausted dialog before drawing. CI, APK
checks and device acceptance are distinct. See the investigation below.

Keep five public patch names. Under **CamScanner disable ads**, `rewardedVideos`
provides meaningful behavior choices rather than a low-level manifest-cleanup bool:

- `block` (default): retain the existing V12 behavior; no shortcut.
- `skip-lottery`: experimental, pristine 7.27.0 (72701) only, online only.

The new mode does not change server authentication, quotas, response parsing, prize
amounts or balances, and does not loop/replay reward requests. The actual backend
may refuse a code or subsequent reward; no guaranteed receipt is claimed.

## Measured flow

`WebAction` maps exact `watchad_lottery` to `WatchAdControl` backed by
`LeftRewardVideoManager`, not the document-function reward manager.
`watch_option_ad` uses a different manager and remains outside this experiment.

Normal reward completion sets the per-listener completion flag; closing the video
then invokes the original `IntervalTaskStateManager` task request. That original
path obtains `lottery_code` from `/reward/task_handle` with `get_lottery_code`,
forwards the actual response through the existing callback, and constructs the
original request-correlated WebView result. This is not a client-only prize flag.
No credentialed requests or real reward claims were made during static inspection.

## Candidate intervention and guards

- Only exact supported version/code and measured pristine callback-path hashes.
- Two UI-thread paths: the existing main-thread branch and its posted Runnable
  target. Place the main prefix *after* its branch-target instruction so the
  original jump cannot skip it. Preserve original register/body/handler graphs.
- Only the exact lottery action, LeftRewardVideoManager and nonempty request id
  are eligible. Unrelated/malformed requests retain the original behavior.
- Null/destroyed/finishing activity, or missing/detached/removing WebView cancels
  the shortcut.
- Construct the original listener with the original controller/activity/manager/
  WebView/request; signal its local completion once and close once, then return.
  The measured parent completion/close handlers are no-ops and accept the null ad
  request; normal code lookup, response handling, JS correlation and cleanup remain
  unmodified. No ad load/show is invoked in this shortcut.
- This is one forward per incoming eligible native action, not a global request-id
  deduplication or a server-idempotency guarantee. No background loop is added.
- Offline plus skip and unqualified old-version skip reject without an APK.

## Validation plan

Execute assembled prefixes for both register layouts: matching action, null/empty
request, other action/manager, lifecycle cancellation, exact constructor bindings,
and one completion/close with no original ad path. Unit guards cover version/code,
register shape and enum option validation.

Official build/catalog checks must keep five names and the block default. Apply
block/skip/combined cases to pristine input, compare all targeted original methods,
login/Word/helper/native/manifest inventories, and retain old block behavior.
Reject unsupported versions, altered callback paths, offline conflict and reapply.
Only a phone test can establish that this experiment reaches the task screen and
that a real reward is granted; a callback/status or compilation is insufficient.

## Qualified test artifact

Source **1af3ed1** passed official Verify39647427001 / Release39647419853:
95 tests, zero failures/errors/skips, including the assembled-prefix branch tests.
Exact MPP SHA256:
`306df074885fc84690206adf086b865f527cd58f2b430391a4ab325768203a14`.
Generated metadata retains five names, three recommendation defaults and the
`rewardedVideos=block` option default.

Four actual APK cases passed: 72701 block, 72701 skip-only, 72701 combined
local-premium/privacy/ads-skip, and 72651 block regression. The block outputs'
canonical target fingerprints equal previously qualified golden outputs.

- Skip modifies exactly the two dispatch methods; their original instruction,
  register, branch and handler graphs remain intact outside the guarded prefix.
  Original completion/close, server-code request/response and JS callbacks remain
  bytecode-identical. The inserted route constructs one correctly bound original
  listener and signals completion/close once, with no ad-load/show call or loop.
- 336 new-target / 332 old-target login methods preserve their two existing cache
  hooks; 2,586 other guarded method graphs retain only expected policy prefixes
  (165 block/skip-only, 218 combined). Four original Word class pins are unchanged.
- Identity/classes/assets/native inventories are preserved except the existing
  exact two signing edits and one existing stateless helper. No new helper class,
  diagnostic overlay or scanner edit is introduced.
- Old-version skip, offline+skip, a targeted callback-constructor NOP mutation and
  reapply reject for the expected reason, CLI exit1, no APK. The mutation retains
  all 109 base guarded definitions, proving the extra callback guard is exercised.

No account credentials or real reward requests were used for these checks.
**Phone acceptance must verify actual reward receipt, not just a status callback.**
Enter the page without pressing first to check for unintended automatic behavior;
then invoke one lottery action and inspect the actual reward/history/balance. The
native action is scoped, but it is not proof of a user gesture or server idempotency.

## Pre-draw failure investigation and preload guard

The official `/lottery/relottery` route loads `RelotteryView-I2SNRQ5F.js` from the
`camscanner-mo` static application. Do not conflate it with the separate
`/integral/intergralTurntable` page. Its `mounted()` calls `watchad_lottery` with
`prestrain: 1` before a draw. An actual claim's `getGift()` calls the same bridge
with `prestrain: 0`, after the draw returns its verification token.

V13 scoped by action, manager and request id, but omitted the original preload
predicate. A generated request id does not establish a user gesture. The corrected
prefix calls the pinned original `WatchAdControl.〇O888o0o(String)` predicate and
returns without loading an ad, constructing a listener or signaling completion
when it reports preload/cancellation. Both UI-thread dispatches use this guard.

Offline replay of the fetched actual component object established that entering
it invokes one preload without a click. Its quota refresh independently converts
`watchad_vip_chance` with unary `+`: numeric 3 permits drawing, whereas both 0 and
null immediately show the exhausted dialog. The API adapter maps a missing chance
to null. This proves an error/zero ambiguity, not the device's actual API response.
The native completion callback also selects status from the completion flag,
without requiring a non-null server code. These are separate issues, not evidence
that the backend consumed a chance or granted a reward.

Validation must cover preload with a valid bridge-generated id on both register
layouts, original-parser cancellation, normal active requests and existing
unrelated/lifecycle guards. Preserve all original methods outside the prefixes.
Phone recovery, real quota and reward receipt remain unverified. Do not fabricate
quota, verification tokens, codes or server success to mask the failure.
