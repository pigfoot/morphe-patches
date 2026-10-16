package app.pigfoot.patches.camscanner

import app.morphe.patcher.patch.*

internal data class SigningEdit(val hash: String, val offset: Int, val before: ByteArray, val after: ByteArray)
internal val signingEdits = mapOf(
    "lib/arm64-v8a/libnative-encrypt.so" to SigningEdit(
        "d53a40d72454dd0e663943a2cf5f209c5d1e8c68bbfc2ff0783d4d5cd58e42c9", 0x2b9d4,
        byteArrayOf(0xb7.toByte(), 0x07, 0x00, 0x36), byteArrayOf(0x1f, 0x20, 0x03, 0xd5.toByte())),
    "lib/armeabi/libnative-encrypt.so" to SigningEdit(
        "2756ca6897741891c047b84099333a2fd3d79fff2251f55983dd7e1c6c0497f8", 0x10ac6,
        byteArrayOf(0x4b, 0xd0.toByte()), byteArrayOf(0x00, 0xbf.toByte()))
)

internal fun editSigning(bytes: ByteArray, spec: SigningEdit): ByteArray {
    require(sha256(bytes) == spec.hash) { "Unsupported or already modified native signing library" }
    require(spec.before.isNotEmpty() && spec.before.size == spec.after.size &&
        spec.offset >= 0 && spec.offset <= bytes.size - spec.before.size) { "Invalid signing edit bounds" }
    require(bytes.copyOfRange(spec.offset, spec.offset + spec.before.size).contentEquals(spec.before)) {
        "Unsupported native signing gate"
    }
    return bytes.copyOf().also { spec.after.copyInto(it, spec.offset) }
}

val camscannerSigningPatch = resourcePatch(
    name = "CamScanner account service signing compatibility",
    description = "Experimental local re-signing compatibility for exact native binaries: retain the original request-signing computation instead of an empty signature after a local signer mismatch. Preserve official API Center discovery, salts, authentication and server results. No host override or global signature spoofing. Phone login unverified.",
    default = false
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED
    }
    dependsOn(camscannerInputPatch, camscannerSideloadStartupPatch)
    execute {
        val libs = listApkEntries("lib/").filter { it.endsWith("/libnative-encrypt.so") }
        require(libs.isNotEmpty() && libs.all { it in signingEdits }) { "Unqualified native signing ABI" }
        // Validate every ABI before writing any payload. Only the caller's
        // skip-computation branch changes; checkSignature and other callers,
        // the original getSalt/MD5/JNI routines and all network code stay intact.
        val replacements = libs.associateWith { editSigning(get(it).readBytes(), signingEdits.getValue(it)) }
        replacements.forEach { (path, bytes) -> get(path).writeBytes(bytes) }
    }
}
