# PikieWalker reward playback

## Scope and validation plan

Target: original PikieWalker 2.0.4 (42), arm64-v8a APKS, Android 12L+.
Motivation: obtain the normal local app reward without playing a reward video.
The application delegates four reward entry points to its own
`AdManager.showAd(Activity, Function1)`. Its result enum has SUCCESS, CANCELLED
and FAILED. The existing ViewModel handles rewards and persistence.

The patch invokes the supplied callback once with the existing SUCCESS enum and
returns before the SDK display/loading/waiting path. It does not inject code into
the Google SDK, add an extension, alter Health Connect/steps, change energy amounts,
disable cooldowns or bypass daily claim eligibility. Confirmation UI and ad
preloading outside showAd are unchanged. It is not a general-purpose ad blocker.

Before mutation, validate package/version/build, original certificate and canonical
class hashes for AdManager and AdResult. Unsupported/modified inputs must fail
without mutation. No runtime helper is necessary: the shared loaded-SDK reward
helper's contract differs from this app-owned enum callback.

Required validation: original input application; only showAd changed; unsupported,
modified and second application rejection; bundle unselected no-op; callback once,
no fall-through/no exception replay; original other methods/classes, manifests,
assets/natives and label preservation. A build is not phone acceptance. Phone
checks include cold start, each available reward entry, reward count and restart
persistence, plus ordinary walking/Health Connect behavior.

## Source preflight results

- Local compilation and JUnit: 112 tests pass, including three new policy tests.
- Release-tool regression: seven release rules and nine JavaScript tests pass.
- Full local Gradle remains unavailable because the existing Morphe Gradle plugin
  1.3.4 cannot resolve in this environment; use the established official-CI route.
- Official MPP application/preservation/negative qualification and phone acceptance
  are distinct gates. Pending results must not be reported as passed.

Status: implementation candidate, not yet qualified or delivered for installation.
