package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val NATIVE_BRIDGE = "Lcom/intsig/webview/WebViewFragment\$JSMethods;"
/** Independently measured pristine 72701 encodings, not additional supported versions. */
internal val nativeBridgePins72701 = mapOf(
    NATIVE_BRIDGE to setOf("1389e44bf3dbffabb352e3a6de80d04daa96476b4f9426d7d63595dc6fd4f2b8", "bc50b67a15ca3ca897b032acbb407ac9f788b5d04090fd37865abff34dbe4a67"),
    "Lcom/intsig/webview/WebViewFragment;" to setOf("8d1a3206477976c6d4fc8a8349d88c827e8fb34cd3b2b8a40253bf3bdd3d80b3", "b3320de170ce156f4aaf3f7f4181b1140e7448ab947c8f191c73fdb3d6ebdbf8"),
    "L〇88O0888/〇00;" to setOf("f8781f3d35506dd06fe27529ee6f31e57a5b30eb2fcf0619be91dbbc2caef94a", "16ea051da41ee0c72c1961afd45d031d5d282e7ac06adcef94e832f6527678e8")
)

/** v0/v1 are scratch; original p0/p1 and the caught Throwable v2 are never overwritten. */
internal fun nativePreloadEntry(): String = """
const-string v0, "CS-NATIVE-V25:ENTRY"
if-eqz p1, :entry_end
invoke-virtual {p1}, Ljava/lang/String;->length()I
move-result v0
const/16 v1, 0x2000
if-gt v0, v1, :entry_end
new-instance v0, Lorg/json/JSONObject;
invoke-direct {v0, p1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
const-string v1, "action"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
move-result-object v1
const-string v0, "watchad_lottery"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v0
if-eqz v0, :entry_end
new-instance v0, Lorg/json/JSONObject;
invoke-direct {v0, p1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
const-string v1, "data"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;
move-result-object v0
if-eqz v0, :entry_end
const-string v1, "prestrain"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I
move-result v0
const/4 v1, 0x1
if-ne v0, v1, :entry_end
const-string v1, "{\"__cs_native_preload_v25\":\"ENTERED\"}"
iget-object v0, p0, Lcom/intsig/webview/WebViewFragment${'$'}JSMethods;->〇o00〇〇Oo:Lcom/intsig/webview/WebViewFragment;
invoke-virtual {v0, v1}, Lcom/intsig/webview/WebViewFragment;->〇0oO(Ljava/lang/String;)V
:entry_end
nop
goto/16 :entry_after
:entry_ignored
move-exception v0
:entry_after
nop
""".trimIndent()

internal fun nativePreloadError(): String = """
    move-exception v2
const-string v0, "CS-NATIVE-V25:ERROR"
if-eqz p1, :error_end
invoke-virtual {p1}, Ljava/lang/String;->length()I
move-result v0
const/16 v1, 0x2000
if-gt v0, v1, :error_end
new-instance v0, Lorg/json/JSONObject;
invoke-direct {v0, p1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
const-string v1, "action"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;
move-result-object v1
const-string v0, "watchad_lottery"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v0
if-eqz v0, :error_end
new-instance v0, Lorg/json/JSONObject;
invoke-direct {v0, p1}, Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V
const-string v1, "data"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;
move-result-object v0
if-eqz v0, :error_end
const-string v1, "prestrain"
invoke-virtual {v0, v1}, Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I
move-result v0
const/4 v1, 0x1
if-ne v0, v1, :error_end
invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
move-result-object v0
invoke-virtual {v0}, Ljava/lang/Class;->getName()Ljava/lang/String;
move-result-object v0
const-string v1, "java.lang.NullPointerException"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_0
const-string v1, "java.lang.IllegalArgumentException"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_1
const-string v1, "java.lang.IllegalStateException"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_2
const-string v1, "java.lang.SecurityException"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_3
const-string v1, "java.lang.VerifyError"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_4
const-string v1, "java.lang.NoClassDefFoundError"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_5
const-string v1, "java.lang.NoSuchMethodError"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_6
const-string v1, "java.lang.UnsatisfiedLinkError"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_7
const-string v1, "java.lang.ExceptionInInitializerError"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :threw_8
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_OTHER\"}"
goto/16 :threw_emit
:threw_0
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_NULL_POINTER\"}"
goto/16 :threw_emit
:threw_1
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_ILLEGAL_ARGUMENT\"}"
goto/16 :threw_emit
:threw_2
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_ILLEGAL_STATE\"}"
goto/16 :threw_emit
:threw_3
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_SECURITY\"}"
goto/16 :threw_emit
:threw_4
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_VERIFY\"}"
goto/16 :threw_emit
:threw_5
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_CLASS_MISSING\"}"
goto/16 :threw_emit
:threw_6
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_METHOD_MISSING\"}"
goto/16 :threw_emit
:threw_7
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_LINK_ERROR\"}"
goto/16 :threw_emit
:threw_8
const-string v1, "{\"__cs_native_preload_v25\":\"THREW_INITIALIZER\"}"
goto/16 :threw_emit
:threw_emit
iget-object v0, p0, Lcom/intsig/webview/WebViewFragment${'$'}JSMethods;->〇o00〇〇Oo:Lcom/intsig/webview/WebViewFragment;
invoke-virtual {v0, v1}, Lcom/intsig/webview/WebViewFragment;->〇0oO(Ljava/lang/String;)V
invoke-virtual {v2}, Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;
move-result-object v0
array-length v1, v0
if-eqz v1, :error_end
const/4 v1, 0x0
aget-object v0, v0, v1
invoke-virtual {v0}, Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;
move-result-object v0
const-string v1, "com.intsig.webview.WebViewFragment${'$'}JSMethods"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_0
const-string v1, "com.intsig.router.RouterServiceManager"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_1
const-string v1, "com.intsig.camscanner.web.WebAction"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_2
const-string v1, "com.intsig.camscanner.attention.WatchAdControl"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_3
const-string v1, "com.intsig.advertisement.adapters.positions.reward.video.LeftRewardVideoManager"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_4
const-string v1, "com.intsig.advertisement.adapters.positions.reward.video.RewardVideo"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_5
const-string v1, "com.intsig.advertisement.adapters.AbsPositionAdapter"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_6
const-string v1, "org.json.JSONObject"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_7
const-string v1, "kotlin.jvm.internal.Intrinsics"
invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
move-result v1
if-nez v1, :site_8
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_OTHER\"}"
goto/16 :site_emit
:site_0
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_BRIDGE_ENTRY\"}"
goto/16 :site_emit
:site_1
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_ROUTER_SERVICE\"}"
goto/16 :site_emit
:site_2
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_APP_DISPATCH\"}"
goto/16 :site_emit
:site_3
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_WATCH_CONTROL\"}"
goto/16 :site_emit
:site_4
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_LEFT_MANAGER\"}"
goto/16 :site_emit
:site_5
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_REWARD_BASE\"}"
goto/16 :site_emit
:site_6
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_POSITION_ADAPTER\"}"
goto/16 :site_emit
:site_7
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_JSON_PARSER\"}"
goto/16 :site_emit
:site_8
const-string v1, "{\"__cs_native_preload_v25\":\"SITE_NULL_CHECK\"}"
goto/16 :site_emit
:site_emit
iget-object v0, p0, Lcom/intsig/webview/WebViewFragment${'$'}JSMethods;->〇o00〇〇Oo:Lcom/intsig/webview/WebViewFragment;
invoke-virtual {v0, v1}, Lcom/intsig/webview/WebViewFragment;->〇0oO(Ljava/lang/String;)V
:error_end
nop
goto/16 :error_after
:error_ignored
move-exception v0
:error_after
nop
    throw v2
""".trimIndent()

internal fun installNativePreloadCapture(method: MutableMethod) {
    val original = requireNotNull(method.implementation)
    require(method.definingClass == NATIVE_BRIDGE && method.name == "callApp" &&
        method.parameterTypes.map { it.toString() } == listOf("Ljava/lang/String;") &&
        method.returnType == "V" && !AccessFlags.STATIC.isSet(method.accessFlags) &&
        original.registerCount == 5 && original.tryBlocks.isEmpty() &&
        original.instructions.last().opcode == Opcode.RETURN_VOID) { "Unsupported native preload entry" }
    val originalCount = original.instructions.size
    method.addInstructions(0, nativePreloadEntry())
    val originalStart = original.instructions.size - originalCount
    val originalEnd = original.instructions.size
    // Appended local branches must be relocated to their actual method offset.
    method.addInstructionsWithLabels(originalEnd, nativePreloadError())
    val code = original.instructions.toList()
    for (stage in listOf("ENTRY", "ERROR")) {
        val start = code.indices.single {
            (((code[it] as? ReferenceInstruction)?.reference as? StringReference)?.string) == "CS-NATIVE-V25:$stage"
        }
        val handler = (start + 1 until code.size).first {
            code[it].opcode == Opcode.MOVE_EXCEPTION && (code[it] as OneRegisterInstruction).registerA == 0
        }
        original.addCatch("Ljava/lang/Throwable;", original.newLabelForIndex(start),
            original.newLabelForIndex(handler - 1), original.newLabelForIndex(handler))
    }
    original.addCatch("Ljava/lang/Throwable;", original.newLabelForIndex(originalStart),
        original.newLabelForIndex(originalEnd), original.newLabelForIndex(originalEnd))
}
