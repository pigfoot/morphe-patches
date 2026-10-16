package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import com.android.tools.smali.dexlib2.writer.pool.DexPool

private const val ACCOUNT_HOSTS = "Lcom/intsig/CsHosts;"
private const val OLD_ACCOUNT_FALLBACK = "https://api-cs-bak.intsig.net/user/cs"
private const val PRIMARY_ACCOUNT_FALLBACK = "https://api-cs.intsig.net/user/cs"

val camscannerAccountServicePatch = bytecodePatch(
    name = "CamScanner account API fallback compatibility",
    description = "Experimental: use the existing catalog primary account service only when production UAPI discovery has no usable configuration. Preserve configured hosts, sandbox and authentication decisions. Phone login unverified.",
    default = false
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { installer, _ ->
        if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED
    }
    dependsOn(camscannerInputPatch)
    execute {
        val pool = DexPool(Opcodes.forDexVersion(39))
        pool.internClass(classDefBy(ACCOUNT_HOSTS))
        val store = MemoryDataStore()
        pool.writeTo(store)
        require(sha256(store.data) == "45d89f62e47907ea096670b644816aff9e989c4331ea90c0d45ae99216ec9d79") {
            "Unsupported or already modified CamScanner account hosts"
        }
        // Both getters first return a nonblank server-configured UAPI. Only
        // their final production fallback constant changes; sandbox is retained.
        val sites = listOf("o0ooO", "〇00〇8").map { name ->
            val method = mutableClassDefBy(ACCOUNT_HOSTS).methods.single {
                it.name == name && it.signature() == "()Ljava/lang/String;"
            }
            val impl = requireNotNull(method.implementation)
            val code = impl.instructions.toList()
            val index = code.indices.single {
                (code[it] as? ReferenceInstruction)?.reference.toString() == OLD_ACCOUNT_FALLBACK
            }
            require(impl.registerCount == 2 && code[index].opcode == Opcode.CONST_STRING &&
                (code[index] as OneRegisterInstruction).registerA == 0 &&
                code[index + 1].opcode == Opcode.RETURN_OBJECT &&
                (code[index + 1] as OneRegisterInstruction).registerA == 0 &&
                code.any { (it as? ReferenceInstruction)?.reference.toString() ==
                    "https://api-cs-bak-sandbox.intsig.net/user/cs" }) {
                "Unsupported CamScanner account fallback body"
            }
            method to index
        }
        // All input definitions and both sites are qualified before mutation.
        sites.forEach { (method, index) ->
            method.replaceInstruction(index, "const-string v0, \"$PRIMARY_ACCOUNT_FALLBACK\"")
        }
    }
}
