# CamScanner lottery video skip experiment

## Motivation and status

The existing disable-ads policy intentionally blocks rewarded video entry points.
A failed video-load dialog is consistent with that policy, not a login regression.
A new explicit request is to skip the video while still allowing the original
lottery/task reward flow. A local completion is not proof of a server credit.
This is a candidate experiment; CI, APK checks and device acceptance are distinct.

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
- Null/destroyed/finishing activity or missing WebView cancels the shortcut.
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
