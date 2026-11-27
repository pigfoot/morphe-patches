package app.pigfoot.patches.kingnet

import app.morphe.patcher.patch.*
import app.pigfoot.patches.shared.certificate.requireOriginalCertificate
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

private val pushTarget = Compatibility(packageName = KINGNET_PACKAGE, name = "智生活", apkFileType = ApkFileType.APK,
    targets = listOf(AppTarget(KINGNET_VERSION, versionCodes = mapOf(
        SupportedAbi.ARM64_V8A to KINGNET_CODE.toInt(), SupportedAbi.ARMEABI_V7A to KINGNET_CODE.toInt(),
        SupportedAbi.X86_64 to KINGNET_CODE.toInt(), SupportedAbi.X86 to KINGNET_CODE.toInt()),
        minSdk = 30, isExperimental = true)))

val kingnetPushFilterPatch = bytecodePatch(
    name = "智生活: Block platform message notifications (experimental)",
    description = "Experimental. Stop the promotional platform messages from showing as phone notifications in the original 智生活 4.21.0 APK. Parcel, order, announcement and community notifications are not changed. It only recognizes messages the app itself labels as platform messages, so some promotions may still arrive. You must uninstall the original app and sign in again.",
    default = false
) {
    compatibleWith(pushTarget)
    availability { installer, _ -> if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.ENABLED }
    extendWith("extensions/kingnet-push.mpe")
    execute {
        requireKingnetTarget(packageMetadata.packageName, packageMetadata.versionName, packageMetadata.versionCode)
        requireOriginalCertificate(packageMetadata.signingCertificates.values.flatten(), KINGNET_SIGNER)
        val pool = DexPool(Opcodes.forDexVersion(39))
        pool.internClass(classDefBy(KINGNET_FCM_SERVICE))
        val store = MemoryDataStore()
        pool.writeTo(store)
        val actual = kingnetSha256(store.data)
        require(actual == KINGNET_FCM_SERVICE_PIN) { "Unsupported or already modified push service" }
        val method = mutableClassDefBy(KINGNET_FCM_SERVICE).methods.single {
            it.name == "handleIntent" && it.parameterTypes.map { type -> type.toString() } == listOf("Landroid/content/Intent;")
        }
        applyKingnetPushFilter(method)
    }
}
