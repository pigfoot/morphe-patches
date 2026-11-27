# 智生活 (Kingnet) 4.21.0 implementation notes

Qualified input: `chk.kingnet.app` 4.21.0 (421004), full APK, signer SHA-1
`6b7853fdb77120ce00bf4148d059db9f1515c04d`. The app is a Cordova hybrid; all edits are in
`assets/www/js/app.js` (SHA-256 pinned to the original).

## Why
Ads are first-party content delivered by one API (`POST /AppUser/GetAd`) that the server
hides only for VIP accounts. Marketing messages come from the third-party Infobip service
and a locally scheduled re-engagement reminder. A local patch cannot make the server treat the
account as VIP, so the client stops using this content instead.
The notification box also has a "platform messages" tab filled by server broadcasts (partner
promotions); the client stops showing it for the same reason.

## Edits (each anchor must match exactly once; unrelated bytes are preserved)
1. Both `GetAd` success handlers no longer commit the response to the ad store, so every ad
   getter stays empty. The request and its callers are unchanged.
2. The Infobip `initialize` function returns before `MobileMessaging.init`; the Infobip
   ready flag stays false, so the personalization/data-sync path never runs.
3. The re-engagement scheduler keeps `cancelAll` but returns before scheduling.
4. The notification box "platform messages" tab is server-broadcast promotion content
   (`NotificationBox/LoadNews`). The first list load keeps it an empty list (not null: the page shows a loading skeleton
   while any list is null), the paged request
   resolves to an empty list, and both unread-count paths (`RegisterCache` list load and
   `CountUnread`) zero the news counter before it is stored, so the tab shows nothing and
   contributes no badge. The notice ("my reminders") and shared-circle tabs are unchanged.

Not changed: FCM registration and handling (parcel, order, intercom and other feature
notifications), billing, VIP state, manifest, native libraries and every other asset.

## Validation
Unit tests cover target pins, asset hash pin, exact-once sites, round trip, rejection of
missing/duplicate/already-modified sites and malformed UTF-8. The official MPP is applied to
the original APK and compared with it: only the JavaScript asset may differ.
Whether Google/Facebook sign-in, payment SDK and root checks accept the re-signed APK, whether
parcel notifications still arrive and whether ads are gone are phone acceptance checks.

## Experimental push filter ("Block platform message notifications")

Server-broadcast promotions also arrive as phone notifications. They are shown by the app's
push plugin or the Firebase SDK, not by `app.js`, so the JavaScript patch cannot stop them.

- Static evidence: the app's JavaScript treats a push whose `FeatureName` is `Default` as a
  platform message (the notification box opens the "platform messages" tab for it; the
  handler shows a toast or opens `Details.FeatureName`). Every other value (`Msg`, `Ann`,
  `vote`, order and community features) belongs to the other tabs and features.
- `com.adobe.phonegap.push.FCMService` is the only effective messaging service (it also
  forwards Infobip messages itself) and already overrides `handleIntent(Intent)`, which runs
  before both the Firebase SDK display and `onMessageReceived`.
- The patch inserts a guard at the start of that method: read the `FeatureName` string extra,
  and return when `PushFilter.isPlatformNews` says it equals `Default`. The helper is a stateless
  Java class in `extensions/kingnet`. The original body is otherwise untouched.
- Input guards: package/version/build, signer, SHA-256 of the re-serialized `FCMService`
  class, and the exact instruction list and register count of `handleIntent` (nothing is
  changed unless all match; a second application is rejected).
- Not verified: that the promotions use `Default` in the live payload, and whether
  `Default` is ever used for a message the user wants. The patch is off by default and its
  target is marked experimental. Infobip messages (`org_ib_d`) are not affected by this
  patch; Infobip is stopped by the first patch.
