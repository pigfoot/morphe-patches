# PiTT GmsCore sign-in adapter

## Qualified reference and selection

Target: PiTT 12.0.0 / 40892 ARM64 APKS. The independent opt-in patch
**PiTT GmsCore Google sign-in** requires separately installed
[MicroG-RE 7.2.2](https://github.com/MorpheApp/MicroG-RE/releases/tag/7.2.2),
compiled package `app.revanced.android.gms`. Its legacy Google Sign-In activity
and service match PiTT's existing `GoogleSignInOptions` / `GoogleSignInAccount`
flow. No app/SDK stub, replacement Kotlin runtime or new extension is bundled.
The user reported successful login after the scoped Firebase Auth correction.
The [historical survey](pitt-google-signin-study.md) records alternatives and why
an existing browser fallback was not assumed.

## Implementation contract

Pin seven original classes, measured independently on pristine base and untouched
merged input, plus the shared PiTT clean-input guard. Retain original package,
Google client audience, Firebase project and account-result parsing.

1. Select the legacy API mode in the qualified availability method; keep its
   original body/handlers rather than returning a synthetic account.
2. Redirect only the three qualified intent/service auth methods. Add two public
   synthetic instance overrides to the legacy sign-in client for service package
   and availability. The shared GmsClient superclass is unchanged.
3. Add one package-visibility query and two original-identity metadata entries for
   MicroG; reject existing/conflicting additions before changing the manifest.
4. Correct the separate pinned Firebase Auth HTTP certificate header with one
   constant in the existing value register, following the SDK's uppercase SHA-1
   representation. See [Firebase identity](pitt-firebase-client-identity.md).

In total, five existing methods change and two client methods are added. The
header correction adds no method/register. All original instructions outside the
qualified mode/string/header sites, branches and exception paths are retained.
The other local sponsored policy is not a dependency. Both policies have no
parameters, and Google sign-in remains unselected by default.

## Boundaries

MicroG account selection alone does not establish Firebase or PiTT login. Keep
real Google ID-token issuance for the original audience, Firebase credential
exchange and original PiTT account-state handling. Do not fake success or disable
TLS, App Check, token/receipt validation. Billing, push, advertisements, Remote
Config, Installations and other GMS clients are not globally redirected.

MicroG identity metadata does not itself change the separate Firebase Auth SDK's
HTTP certificate header. Installing MicroG alone does not patch PiTT. Original
Play services and the separately packaged MicroG variant are not interchangeable
without this app-scoped adapter. Root/mount and other MicroG variants are not
qualified by this implementation.

## Updating and validating

Start from original v12 APKS, keep the same package and Morphe signing key, and
explicitly select the adapter. Removing temporary diagnostics does not remove
this adapter or its Firebase compatibility fix. Stock failure messages/callbacks
are retained; no diagnostic checkbox, formatter or additional failure text ships.

Follow the [implementation validation frame](pitt-implementation.md). Verify exact
class pins and inherited client method contracts; reject missing/modified classes,
collisions and reapplication. Invert the declared edits to recover original method
graphs and independently verify manifest/resource/native preservation. New device,
account, cancellation, network or expired-token behavior requires actual runtime
observation; prior user acceptance is not a universal compatibility claim.
