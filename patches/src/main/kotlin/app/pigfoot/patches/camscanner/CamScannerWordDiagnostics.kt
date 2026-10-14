package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore

private const val TRACE = "Lapp/pigfoot/diagnostics/WordTrace;"
private const val BUILDER = "Lcom/intsig/app/AlertDialog\$Builder;"
private const val PRESENTER = "Lcom/intsig/camscanner/pagelist/presenter/WordListPresenter;"
private const val IMAGE = "Lcom/intsig/camscanner/image2word/ImageToWordFragment;"
private const val FACADE = "Lcom/intsig/camscanner/jsondoc/share/BaseJsonDocConverterFacade;"
private const val CALLABLE = "Lcom/intsig/camscanner/pic2word/presenter/Json2WordCallable;"
private val diagnosticPins = mapOf(
    PRESENTER to "6655527113faa81e40bb2a350d63c300beec6727a8948b212ba153f52f22a2c8",
    IMAGE to "6090f727a3266daa3f6662575c23dbb5ed95bf97fd95f778c61dbbb4e28f18ea",
    FACADE to "b210927af345dd5b03cfde31bac2e4f993357411263323ab81b776b56f802fe7",
    CALLABLE to "3fe946365075f021df84c89d1482200dc1b77e0bf6dce4c937782ecf27182e4c"
)

val camscannerWordDiagnosticsPatch = bytecodePatch(
    name = "CamScanner Word export diagnostics",
    description = "Temporary v2 diagnostics for three Word result-103 dialogs and recent numeric HTTP error-header observations. Process-local events are not document-correlated. Does not change quotas, requests or results.",
    default = false
) {
    compatibleWith(Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
            SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true))))
    availability { installer, _ -> if (installer == InstallerType.MOUNT) PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED }
    dependsOn(camscannerInputPatch)
    extendWith("extensions/camscanner-word-diagnostics.mpe")
    execute {
        // Reject v1/v2/already-modified classes before any instruction mutation.
        for ((type, expected) in diagnosticPins) {
            val pool = DexPool(Opcodes.forDexVersion(39))
            pool.internClass(classDefBy(type))
            val store = MemoryDataStore()
            pool.writeTo(store)
            require(sha256(store.data) == expected) { "Unsupported or already instrumented Word class: $type" }
        }
        val dialogs = listOf(
            Triple(PRESENTER, "o8〇", "PRESENTER"),
            Triple(IMAGE, "O80〇", "IMAGE2WORD"),
            Triple(FACADE, "〇0000OOO", "JSONDOC")
        )
        for ((type, name, label) in dialogs) {
            val descriptor = when (type) {
                PRESENTER -> "($type)V"
                IMAGE -> "()V"
                else -> "($type" + "Landroid/app/Activity;)V"
            }
            val method = mutableClassDefBy(type).methods.single { it.name == name && it.signature() == descriptor }
            val impl = requireNotNull(method.implementation)
            val ins = impl.instructions.toList()
            val messageIndex = ins.indexOfFirst {
                it is ReferenceInstruction && it.reference.toString() == "$BUILDER->OO0o〇〇(I)$BUILDER"
            }
            require(messageIndex >= 0 && ins[messageIndex + 1].opcode == Opcode.MOVE_RESULT_OBJECT)
            val call = ins[messageIndex] as FiveRegisterInstruction
            val result = (ins[messageIndex + 1] as OneRegisterInstruction).registerA
            val scratch = when (type) { PRESENTER -> 0; IMAGE -> 1; else -> 2 }
            require(impl.registerCount == if (type == IMAGE) 4 else 3)
            require(call.registerCount == 2 && result == if (type == PRESENTER) 2 else if (type == IMAGE) 0 else 1)
            // Scratch is the original resource-id register, overwritten before
            // subsequent use. Keep invoke/move-result adjacency and all old ops.
            require(call.registerD == if (type == PRESENTER) 2 else scratch)
            method.addInstructions(messageIndex + 2, """
                const-string v$scratch, "$label"
                invoke-static {v$scratch}, $TRACE->snapshot(Ljava/lang/String;)Ljava/lang/String;
                move-result-object v$scratch
                invoke-virtual {v$result, v$scratch}, $BUILDER->Oooo8o0〇(Ljava/lang/CharSequence;)$BUILDER
            """.trimIndent())
        }
        val core = mutableClassDefBy(CALLABLE).methods.single {
            it.name == "OO0o〇〇〇〇0" && it.returnType == "Lcom/intsig/camscanner/pic2word/bean/WordResponse;"
        }
        // No arguments/register mutations, file writes, or network interception.
        core.addInstructions(0, "invoke-static {}, $TRACE->begin()V")
        val parser = mutableClassDefBy(CALLABLE).methods.single {
            it.name == "〇o〇" && it.signature() == "(Lokhttp3/Response;)I"
        }
        val ins = requireNotNull(parser.implementation).instructions.toList()
        require(parser.implementation!!.registerCount == 4 && ins[3].opcode == Opcode.MOVE_RESULT_OBJECT &&
            (ins[3] as OneRegisterInstruction).registerA == 3)
        require((ins[2] as ReferenceInstruction).reference.toString() ==
            "Lokhttp3/Response;->O8ooOoo〇(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;")
        // Observe the very same header string; preserve original parsing/value.
        parser.addInstructions(4, "invoke-static {v3}, $TRACE->header(Ljava/lang/String;)V")
    }
}
