package app.pigfoot.patches.gpsjoystick

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.smali.ExternalLabel
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.pigfoot.patches.shared.certificate.requireOriginalCertificate
import app.pigfoot.patches.shared.maps.isMapsKey
import app.pigfoot.patches.shared.maps.replaceMapsKeyBytes
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import java.security.MessageDigest

internal const val GPS_PACKAGE = "com.theappninjas.fakegpsjoystick"
internal const val GPS_PREFS = "Lz3/b0;"
internal const val GPS_RESTORE = "Lg3/e;"
internal const val GPS_KEYS = "Lzd/f0;"
internal const val GPS_SIGNER = "cf5516416b8af05634166a44a3349aa65b346a92"
internal val gpsPins = mapOf(
    GPS_PREFS to "3911667b81f8640654e8cef290c2ef880e86207140cc6ff1fc52da65aa7c0e83",
    GPS_RESTORE to "db04642f4b2e33a43ba2443cd4286fd734e1ee5153aa11baf69a605f652f5b76",
    GPS_KEYS to "68cf2a0ff69b4170bbc18e6b2150f99d55822f77932e3a5d7c22a2c137201df8"
)
internal fun requireGpsTarget(pkg: String?, version: String?, code: String?) {
    require(pkg == GPS_PACKAGE && version == "5.3.3" && code == "3030") { "Unsupported GPS JoyStick package/version/build" }
}
internal fun gpsClassHash(c: ClassDef): String {
    val pool= DexPool(Opcodes.forDexVersion(39));pool.internClass(c)
    val store=MemoryDataStore();pool.writeTo(store)
    return MessageDigest.getInstance("SHA-256").digest(store.data).joinToString("") { "%02x".format(it) }
}

/** Only the two qualified local subscription keys. Every unrelated preference
 * reaches the original first instruction with original inputs intact. */
internal fun enableGpsLocalEntitlements(method: MutableMethod) {
    require(method.definingClass == GPS_PREFS && method.name == "b" && method.accessFlags == 1 &&
        method.parameterTypes == listOf(GPS_KEYS,"Ljava/lang/Object;") && method.returnType == "Ljava/lang/Object;") { "Unsupported GPS preference method" }
    val impl=requireNotNull(method.implementation)
    require(impl.registerCount == 6) { "Unsupported GPS preference registers" }
    method.addInstructionsWithLabels(0,"""
        sget-object v0, $GPS_KEYS->P0:$GPS_KEYS
        if-eq p1, v0, :gps_premium
        sget-object v0, $GPS_KEYS->E0:$GPS_KEYS
        if-ne p1, v0, :gps_original
        :gps_premium
        sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
        return-object v0
    """.trimIndent(),ExternalLabel("gps_original",impl.instructions.first()))
}

/** Normalize only this app's local restore-status consumer. Original writes,
 * callback order and exception behavior remain; no Billing SDK/purchase/token
 * or receipt-validation request is fabricated or altered. */
internal fun keepGpsPremiumOnRestore(method: MutableMethod) {
    require(method.definingClass == GPS_RESTORE && method.name == "e" && method.accessFlags == 1 &&
        method.parameterTypes == listOf("I") && method.returnType == "V") { "Unsupported GPS restore method" }
    val impl=requireNotNull(method.implementation)
    require(impl.registerCount == 8 && impl.tryBlocks.isEmpty()) { "Unsupported GPS restore layout" }
    method.addInstructions(0,"const/4 p1, 0x3")
}

internal val gpsInputPatch = bytecodePatch {
    execute {
        requireGpsTarget(packageMetadata.packageName,packageMetadata.versionName,packageMetadata.versionCode)
        requireOriginalCertificate(packageMetadata.signingCertificates.values.flatten(),GPS_SIGNER)
        for((type,pin) in gpsPins) {
            val c=requireNotNull(classDefByOrNull(type)) { "Missing GPS JoyStick class: $type" }
            require(gpsClassHash(c)==pin) { "Unsupported or already modified GPS JoyStick class: $type" }
        }
    }
}

val gpsJoyStickPremiumPatch = bytecodePatch(
    name = "GPS JoyStick Premium",
    description = "Enable local Premium features and ad-free use. Optionally provide a Maps key for map access. This does not purchase a subscription.",
    default = true
) {
    compatibleWith(Compatibility(packageName=GPS_PACKAGE,name="GPS JoyStick",apkFileType=ApkFileType.APK,
        targets=listOf(AppTarget("5.3.3",versionCodes=mapOf(SupportedAbi.ARM64_V8A to 3030),minSdk=23))))
    availability { installer, _ -> if(installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.ENABLED }
    val mapsKey by stringOption(
        key="mapsApiKey", default="", title="Google Maps API key",
        description="Optional key for map access. Leave blank to keep the app's original key.",
        required=false, validator={value -> isMapsKey(value)}
    )
    dependsOn(rawResourcePatch {
        dependsOn(gpsInputPatch)
        execute {
            if(!mapsKey.isNullOrBlank()) {
                val manifest=get("AndroidManifest.xml")
                val replacement=replaceMapsKeyBytes(manifest.readBytes(),
                    setOf("c56fe8dba41296189e9839849a6db786a3e1403a850c25fccc6d116eb52803c7"),
                    "18e4effd43e80b25d94c45bd641313d180d826d876656f1a93383c9ffc9f8628",mapsKey)
                manifest.writeBytes(replacement)
            }
        }
    })
    execute {
        val prefs=classDefBy(GPS_PREFS).methods.single { it.name=="b" && it.parameterTypes==listOf(GPS_KEYS,"Ljava/lang/Object;") && it.returnType=="Ljava/lang/Object;" }
        val restore=classDefBy(GPS_RESTORE).methods.single { it.name=="e" && it.parameterTypes==listOf("I") && it.returnType=="V" }
        val prepared=listOf(MutableMethod(prefs).also(::enableGpsLocalEntitlements),MutableMethod(restore).also(::keepGpsPremiumOnRestore))
        for(replacement in prepared) {
            val owner=mutableClassDefBy(replacement.definingClass)
            val old=owner.methods.single { it.name==replacement.name && it.parameterTypes==replacement.parameterTypes && it.returnType==replacement.returnType }
            require(owner.methods.remove(old));require(owner.methods.add(replacement))
        }
    }
}
