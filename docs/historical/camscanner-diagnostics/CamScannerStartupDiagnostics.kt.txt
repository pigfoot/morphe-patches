package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

private const val BUILDER = "Lcom/intsig/app/AlertDialog\$Builder;"

val camscannerStartupDiagnosticsPatch = bytecodePatch(
    name = "CamScanner startup warning diagnostics",
    description = "Temporary caller-name trace on the local piracy warning only. Preserves the warning, checks and normal messages. Not a startup fix.",
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
        val pool = DexPool(Opcodes.forDexVersion(39))
        pool.internClass(classDefBy(BUILDER))
        val store = MemoryDataStore()
        pool.writeTo(store)
        require(sha256(store.data) == "5481522c9669baf106ef10dbab2cfef5b3444f2af7943f8d8731e4101604c267") {
            "Unsupported or already instrumented startup dialog builder"
        }
        val method = mutableClassDefBy(BUILDER).methods.single {
            it.name == "Oooo8o0〇" && it.signature() == "(Ljava/lang/CharSequence;)$BUILDER"
        }
        require(requireNotNull(method.implementation).registerCount == 3)
        // p1 is v2; no new registers, branches, handlers, or modified checks.
        method.addInstructions(0, """
            invoke-static {v2}, Lapp/pigfoot/diagnostics/WordTrace;->startupMessage(Ljava/lang/CharSequence;)Ljava/lang/CharSequence;
            move-result-object v2
        """.trimIndent())
    }
}
