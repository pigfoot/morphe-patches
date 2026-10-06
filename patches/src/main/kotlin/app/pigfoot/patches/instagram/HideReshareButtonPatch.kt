package app.pigfoot.patches.instagram

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.literal
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val MEDIA = "Lcom/instagram/feed/media/Media;"
private const val FLAG = "enable_media_notes_production"
private const val FLAG_HASH = -545107410L

// Qualified against the clean 450 getter, not the removed 439 LiveTreeMediaDict.
// Both the cache and the native-backed read converge through this getter.
private object ReshareGetter : Fingerprint(
    definingClass = MEDIA,
    parameters = emptyList(),
    returnType = "Ljava/lang/Boolean;",
    strings = listOf(FLAG),
    filters = listOf(literal(FLAG_HASH)),
    custom = { method, _ ->
        method.implementation?.instructions?.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcom/instagram/pando/livetree/LiveTreeJNI;" &&
                reference.name == "getOptionalBooleanValueNative" &&
                reference.parameterTypes.map { it.toString() } == listOf("I") &&
                reference.returnType == "Ljava/lang/Boolean;"
        } == true
    }
)

val hideInstagramReshareButtonPatch = bytecodePatch(
    name = "Hide Instagram reshare button",
    description = "Experimental Instagram 450 reshare flag override. Preserves package and app name; phone verification is still required.",
    default = false
) {
    compatibleWith(Compatibility(
        name = "Instagram",
        packageName = "com.instagram.android",
        apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget("450.0.0.50.77",
            versionCodes = mapOf(SupportedAbi.ARM64_V8A to 385611400)))
    ))

    execute {
        require(packageMetadata.packageName == "com.instagram.android" &&
            packageMetadata.versionName == "450.0.0.50.77" &&
            packageMetadata.versionCode == "385611400") {
            "Only qualified clean Instagram 450.0.0.50.77 (385611400) is supported"
        }
        val matches = ReshareGetter.matchAll()
        require(matches.size == 1) { "Missing or ambiguous Instagram reshare getter" }
        val method = matches.single().method
        require(method.name == "A3o" && method.implementation!!.registerCount >= 1 &&
            method.implementation!!.instructions.first().opcode == Opcode.IGET_OBJECT) {
            "Unexpected Instagram 450 reshare getter shape"
        }
        method.addInstructions(0, """
            sget-object v0, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;
            return-object v0
        """.trimIndent())
    }
}
