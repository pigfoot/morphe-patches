# CamScanner version-porting workflow

## Current change and validation frame

The candidate keeps pristine 7.26.5 (72651) and adds independently inspected
7.27.0 (72701) APKM inputs. It removes the scanner initialization patch entirely
from compiled sources, without merging its edit into trust. Five public names
remain; recommended defaults remain trust, local premium and privacy.

Success requires official CI, actual APK applications to both pristine targets,
exact allowed method/native/manifest deltas, and modified/wrong-input rejection.
A compiled bundle alone is not phone/login/cloud validation. Qualification results
will be recorded after the actual matrix finishes; this is not yet an acceptance
claim. No stable release or merge is implied.

## Reviewed differences for 72701

- All 107 guarded classes still exist; 100 canonical class hashes are identical.
  Seven class pins change, plus the separately guarded email-login model.
- The login cache lookup result changes from v2 to v3 (10 registers, email v8);
  the coroutine remains byte-identical, including its v4 cache result. New login
  network-error tracking and register allocation are preserved, not replaced.
- Both native signing libraries are byte-identical to 72651; exact old byte/hash
  guards therefore remain valid. This must be measured for every future input.
- Local feature methods keep their identities and instruction bodies. Changes
  elsewhere in AccountPreference/SyncUtil/PreferenceHelper remain untouched.
- Two ad-loading methods retain their contract but reference newly obfuscated
  SDK wrapper classes; the selected suppression policy remains entry-point-only.
  FabricUtils callback wrapper renaming does not change its reviewed signature.
- The independently supplied 72751 APK is packed: shell-only outer DEX, missing
  application targets and different signing native. It remains unsupported.

## Efficient repeatable process

1. Acquire once into private storage; checkpoint any consumable reference before
   fetching chunks. Validate authenticated decryption, exact size, SHA256 and ZIP
   CRC. Never commit share keys, URLs, APKs or signing keys.
2. Inventory identity/version/code/splits/ABIs/certificate. Certificate extraction
   alone is not independent cryptographic signature/provenance validation. Reject
   packed inputs early rather than wasting a build or loosening fingerprints.
3. Merge pristine full inputs once using the qualified Morphe Desktop merger.
   Never derive the trusted baseline from a patched output. Preserve split modules.
4. Run the read-only preflight below. It inventories all outer DEX classes, native
   hashes and canonical class/method definitions for the guarded targets. Review
   only changed targets, including registers, branch/handler graphs and external
   reference changes. The report does NOT auto-generate an acceptance allowlist.
5. After review, add an exact version-name/code profile. Retain old pins and use
   only measured per-version overrides; do not accept a broad version range.
   Verify hook argument/result registers and original control flow independently.
6. Run official Gradle CI, inspect generated catalog/defaults/targets, then apply
   core/defaults/ads/offline/folder-option cases to pristine new inputs and regress
   the old target. Compare identity, classes, unchanged methods, modules/OCR,
   permissions and exact allowed native edits. Test modified/reapply/version
   mismatch rejection; never treat an ignored unknown CLI name as a passed guard.
7. Package the exact CI MPP, generated patch list, hashes and concise instructions.
   Reset old Manager selections; use the same local key for an overlay update.
   Record actual evidence and pending device checks before calling the port done.

## Preflight command

Requires Python 3, Java/Javac 21 and the qualified Morphe Desktop 1.18.1 JAR.
Inputs below are independently merged APKs (not APKM archives).

```sh
python3 scripts/camscanner/preflight.py \
  --baseline /private/old-original-merged.apk \
  --candidate /private/new-original-merged.apk \
  --desktop-jar /private/morphe-desktop.jar \
  --output /private/new-target-report.json
```

Use `--java`/`--javac` for explicit Java 21 binaries. The tool records the Desktop
JAR hash, compiles its read-only helper in a temporary directory, compares full
canonical method definitions (including metadata), and adds instruction graphs
for review. Exit 2 means missing application targets; malformed archives or tool
errors fail nonzero. Exit 0 means analysis completed, **not compatibility**.
Reports contain code and local input evidence; keep them private by default.

The scanner experiment and its old tests are in
`docs/historical/camscanner-scanner/` as text, never compiled or shipped.
