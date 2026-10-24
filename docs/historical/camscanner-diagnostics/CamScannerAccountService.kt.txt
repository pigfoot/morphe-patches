package app.pigfoot.patches.camscanner

import app.morphe.patcher.patch.*

// Retain the old name so stale explicit selections fail clearly rather than
// silently redirecting UAPI to the distinct UAPI_CN catalog service.
val camscannerAccountServicePatch = bytecodePatch(
    name = "CamScanner account API fallback compatibility",
    description = "Withdrawn: replacing UAPI with the UAPI_CN fallback is not region-equivalent. Deselect this option and use account service signing compatibility to preserve official API Center discovery.",
    default = false
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { _, _ -> PatchAvailability.UNAVAILABLE }
    execute { error("Withdrawn account fallback: deselect this option; preserve official API Center discovery with account service signing compatibility") }
}
