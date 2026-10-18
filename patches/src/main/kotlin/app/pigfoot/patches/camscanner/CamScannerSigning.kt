package app.pigfoot.patches.camscanner

import app.morphe.patcher.patch.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

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

private val signingResources = resourcePatch {
    dependsOn(camscannerInputPatch)
    execute {
        val libs = listApkEntries("lib/").filter { it.endsWith("/libnative-encrypt.so") }
        require(libs.isNotEmpty() && libs.all { it in signingEdits }) { "Unqualified native signing ABI" }
        val replacements = libs.associateWith { editSigning(get(it).readBytes(), signingEdits.getValue(it)) }
        replacements.forEach { (path, bytes) -> get(path).writeBytes(bytes) }
    }
}

val camscannerSigningPatch = bytecodePatch(
    name = "CamScanner account service signing compatibility",
    description = "Required compatibility for supported re-signed standard/Shizuku installs: preserve original request signing and recover exact obsolete/error-only API Center caches. Includes V9 diagnostics. No forced host, global signature spoofing, authentication-result changes or INTERNET grant. Root mount is not qualified.",
    default = true
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.REQUIRED
    }
    // Diagnostics owns pristine EmailLoginViewModel validation before its own
    // handler instrumentation. The two cache-lookup methods remain untouched.
    dependsOn(camscannerInputPatch, camscannerSideloadStartupPatch,
        camscannerLoginDiagnosticsPatch, signingResources)
    execute {
        val model = "Lcom/intsig/tsapp/account/viewmodel/EmailLoginViewModel;"
        val fetch = "Lcom/intsig/tsapp/account/viewmodel/EmailLoginViewModel\$fetchAndLoadApisByEmail\$2;"
        val pool = DexPool(Opcodes.forDexVersion(39))
        pool.internClass(classDefBy(fetch))
        val store = MemoryDataStore()
        pool.writeTo(store)
        require(sha256(store.data) == "73f649d182b3d4362ad9ded0a180b7a51db055df8a9ef164b566f7ee038e719a") {
            "Unsupported or already modified login API cache coroutine"
        }
        val sites = listOf(
            Triple(model, "〇O888o0o", listOf(10, 2, 8)),
            Triple(fetch, "invokeSuspend", listOf(5, 4, 4))
        ).map { (type, name, registers) ->
            val method = mutableClassDefBy(type).methods.single { it.name == name }
            val impl = requireNotNull(method.implementation)
            val code = impl.instructions.toList()
            require(impl.registerCount == registers[0]) { "Unsupported login API cache registers" }
            val index = code.indices.single {
                (code[it] as? ReferenceInstruction)?.reference.toString() ==
                    "Lcom/intsig/tianshu/utils/UserInfoSettingUtil;->O8(Ljava/lang/String;)Ljava/lang/String;"
            }
            require((code[index] as? FiveRegisterInstruction)?.let {
                it.registerCount == 1 && it.registerC == registers[2]
            } == true && code[index + 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
                (code[index + 1] as OneRegisterInstruction).registerA == registers[1]) {
                "Unsupported login API cache lookup"
            }
            Triple(method, index + 2, registers[1])
        }
        // Validate both original cache sites before adding the passive policy.
        // A null result follows the existing cache-miss fetch/persist path.
        for ((method, index, result) in sites) method.addInstructions(index, """
            invoke-static {v$result}, Lapp/pigfoot/diagnostics/WordTrace;->refreshLoginApis(Ljava/lang/String;)Ljava/lang/String;
            move-result-object v$result
        """.trimIndent())
    }
}
