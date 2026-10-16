package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

private const val EMAIL_SCENE = "Lcom/intsig/tsapp/account/login/login_dialog_scene/EmailPwdLoginScene;"
private const val LOGIN_ERROR = "Lcom/intsig/tsapp/account/model/ErrorMsg;"
private const val EMAIL_MODEL = "Lcom/intsig/tsapp/account/viewmodel/EmailLoginViewModel;"
private const val LOGIN_EXCEPTION = "Lcom/intsig/tianshu/exception/TianShuException;"
private const val LOGIN_API = "Lcom/intsig/tianshu/TianShuAPI;"

val camscannerLoginDiagnosticsPatch = bytecodePatch(
    name = "CamScanner email login diagnostics",
    description = "Temporary v5 email diagnostics: exact error linkage, HTTP-500 headers and allowlisted query route/client identity, including the original exception wrapper. Unknown hosts are redacted. No credentials, request edits or authentication bypass. Not a login fix.",
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
            EMAIL_MODEL to "e2d0db1e14ccd538a167cf5db55ec360b6379d8f4fd37d3f8942048679cf8121",
            LOGIN_API to "a597209a1b517866d6b7afbf8c8ad3bcd4c1ef8dc6c7e59b1c29aa92e60ba0ca"
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
        val posts = handlerCode.indices.filter {
            (handlerCode[it] as? ReferenceInstruction)?.reference.toString() ==
                "Lcom/intsig/utils/SingleLiveEvent;->postValue(Ljava/lang/Object;)V"
        }
        require(posts.size == 6 && posts.all {
            (handlerCode[it] as? FiveRegisterInstruction)?.let { call ->
                call.registerCount == 2 && call.registerD == 0
            } == true
        })
        val api = mutableClassDefBy(LOGIN_API).methods.single {
            it.name == "〇oo" && it.signature() ==
                "(Ljava/lang/String;Lcom/intsig/tianshu/TianShuAPI\$Ope;IZIZZLjava/lang/String;Ljava/util/Map;)V"
        }
        val apiCode = requireNotNull(api.implementation).instructions.toList()
        require(api.implementation!!.registerCount == 44)
        // This exact constructor/throw follows the HTTP-500 Server-header
        // formatter. The other exception constructors are deliberately untouched.
        val http500 = apiCode.indices.single { index ->
            index >= 20 && index + 1 < apiCode.size &&
                (apiCode[index] as? ReferenceInstruction)?.reference.toString() ==
                    "$LOGIN_EXCEPTION-><init>(ILjava/lang/String;)V" &&
                (apiCode[index] as? FiveRegisterInstruction)?.let {
                    it.registerCount == 3 && it.registerC == 1 && it.registerD == 8 && it.registerE == 0
                } == true &&
                apiCode[index + 1].opcode == Opcode.THROW &&
                (apiCode[index + 1] as? OneRegisterInstruction)?.registerA == 1 &&
                (index - 20 until index).any {
                    (apiCode[it] as? ReferenceInstruction)?.reference.toString() == " headerServer:"
                }
        }
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
        // All original classes and insertion sites have now been validated.
        api.addInstructions(http500 + 1,
            "invoke-static {v1, v11, v8}, Lapp/pigfoot/diagnostics/WordTrace;->loginHttp500(Ljava/lang/Throwable;Ljava/lang/Object;I)V")
        posts.sortedDescending().forEach {
            handler.addInstructions(it,
                "invoke-static {v0}, Lapp/pigfoot/diagnostics/WordTrace;->loginBind(Ljava/lang/Object;)V")
        }
        handler.addInstructions(2,
            "invoke-static {v8, v1}, Lapp/pigfoot/diagnostics/WordTrace;->loginLinkedError(Ljava/lang/Throwable;I)V")
        // v0 already holds exactly the displayed error; v1 is its numeric code.
        // No added registers, credentials, request edits, or changed branches.
        method.addInstructions(getter + 2, """
            invoke-static {v0, v1, v9}, Lapp/pigfoot/diagnostics/WordTrace;->loginLinkedMessage(Ljava/lang/String;ILjava/lang/Object;)Ljava/lang/String;
            move-result-object v0
        """.trimIndent())
    }
}
