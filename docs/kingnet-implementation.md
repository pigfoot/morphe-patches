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
