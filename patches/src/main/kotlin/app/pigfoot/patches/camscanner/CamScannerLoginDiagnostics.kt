package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

private const val EMAIL_SCENE = "Lcom/intsig/tsapp/account/login/login_dialog_scene/EmailPwdLoginScene;"
private const val LOGIN_ERROR = "Lcom/intsig/tsapp/account/model/ErrorMsg;"
private const val EMAIL_MODEL = "Lcom/intsig/tsapp/account/viewmodel/EmailLoginViewModel;"
private const val LOGIN_EXCEPTION = "Lcom/intsig/tianshu/exception/TianShuException;"

val camscannerLoginDiagnosticsPatch = bytecodePatch(
    name = "CamScanner email login diagnostics",
    description = "Temporary v2 email-error diagnostics: numeric codes, fixed path/message hints and app-only source names. Recent process metadata is not request-correlated. No credentials, request edits or authentication bypass. Not a login fix.",
    default = false
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED
    }
    dependsOn(camscannerInputPatch, camscannerDiagnosticExtensionPatch)
    execute {
        for ((type, expected) in mapOf(
            EMAIL_SCENE to "9a4b13dc87853bbdd846a440994ddd848925b930deddc87dc826261e7a603917",
            EMAIL_MODEL to "e2d0db1e14ccd538a167cf5db55ec360b6379d8f4fd37d3f8942048679cf8121"
        )) {
            val pool = DexPool(Opcodes.forDexVersion(39))
            pool.internClass(classDefBy(type))
            val store = MemoryDataStore()
            pool.writeTo(store)
            require(sha256(store.data) == expected) { "Unsupported or already instrumented email login class: $type" }
        }
        val handler = mutableClassDefBy(EMAIL_MODEL).methods.single {
            it.name == "o〇〇0〇" && it.signature() == "($LOGIN_EXCEPTION" +
                "Ljava/lang/String;Lcom/intsig/tsapp/account/model/LoginFromScene;)V"
        }
        val handlerCode = requireNotNull(handler.implementation).instructions.toList()
        require(handler.implementation!!.registerCount == 11 &&
            (handlerCode[0] as? ReferenceInstruction)?.reference.toString() == "$LOGIN_EXCEPTION->getErrorCode()I" &&
            handlerCode[1].opcode == Opcode.MOVE_RESULT && (handlerCode[1] as OneRegisterInstruction).registerA == 1)
        val method = mutableClassDefBy(EMAIL_SCENE).methods.single {
            it.name == "〇O〇80o08O" && it.signature() == "($EMAIL_SCENE$LOGIN_ERROR)Lkotlin/Unit;"
        }
        val impl = requireNotNull(method.implementation)
        require(impl.registerCount == 10)
        val code = impl.instructions.toList()
        val getter = code.indices.single {
            (code[it] as? ReferenceInstruction)?.reference.toString() == "$LOGIN_ERROR->〇080()I"
        }
        require(getter >= 2 && code[getter - 1].opcode == Opcode.MOVE_RESULT_OBJECT &&
            (code[getter - 1] as OneRegisterInstruction).registerA == 0 &&
            (code[getter - 2] as? ReferenceInstruction)?.reference.toString() == "Ljava/lang/Object;->toString()Ljava/lang/String;" &&
            code[getter + 1].opcode == Opcode.MOVE_RESULT &&
            (code[getter + 1] as OneRegisterInstruction).registerA == 1)
        // Both original classes and insertion sites have now been validated.
        handler.addInstructions(2,
            "invoke-static {v8, v1}, Lapp/pigfoot/diagnostics/WordTrace;->loginError(Ljava/lang/Throwable;I)V")
        // v0 already holds exactly the displayed error; v1 is its numeric code.
        // No added registers, credentials, request edits, or changed branches.
        method.addInstructions(getter + 2, """
            invoke-static {v0, v1}, Lapp/pigfoot/diagnostics/WordTrace;->loginMessage(Ljava/lang/String;I)Ljava/lang/String;
            move-result-object v0
        """.trimIndent())
    }
}
