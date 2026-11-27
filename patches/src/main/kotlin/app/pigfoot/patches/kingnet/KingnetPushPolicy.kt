package app.pigfoot.patches.kingnet

import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.smali.ExternalLabel
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

internal const val KINGNET_FCM_SERVICE = "Lcom/adobe/phonegap/push/FCMService;"
internal const val KINGNET_PUSH_FILTER = "Lapp/pigfoot/kingnet/PushFilter;"
internal const val KINGNET_FCM_SERVICE_PIN = "d7790e722d65f3ad5f6c78921cbeb6e130701d30a244e019c7008ad5328986de"

private const val FIREBASE_SERVICE = "Lcom/google/firebase/messaging/FirebaseMessagingService;"
private const val INTENT = "Landroid/content/Intent;"
private const val INTRINSICS = "Lkotlin/jvm/internal/Intrinsics;->checkNotNullParameter(Ljava/lang/Object;Ljava/lang/String;)V"

/** Opcode + operand text of the pristine `FCMService.handleIntent(Intent)` body (4.21.0). */
private val PRISTINE_HANDLE_INTENT = listOf(
    Opcode.CONST_STRING to "intent",
    Opcode.INVOKE_STATIC to INTRINSICS,
    Opcode.CONST_STRING to "gcm.n.event_time",
    Opcode.INVOKE_VIRTUAL to "$INTENT->removeExtra(Ljava/lang/String;)V",
    Opcode.CONST_STRING to "gcm.notification.event_time",
    Opcode.INVOKE_VIRTUAL to "$INTENT->removeExtra(Ljava/lang/String;)V",
    Opcode.INVOKE_SUPER to "$FIREBASE_SERVICE->handleIntent($INTENT)V",
    Opcode.RETURN_VOID to ""
)

private fun operandText(instruction: com.android.tools.smali.dexlib2.iface.instruction.Instruction): String {
    val reference = (instruction as? ReferenceInstruction)?.reference ?: return ""
    return if (reference is StringReference) reference.string else reference.toString()
}

/** Fails closed unless this is exactly the pristine entry point; mutates nothing. */
internal fun requireFcmHandleIntentShape(method: MutableMethod) {
    require(method.definingClass == KINGNET_FCM_SERVICE && method.name == "handleIntent" &&
        method.returnType == "V" && method.parameterTypes.map { it.toString() } == listOf(INTENT) &&
        method.accessFlags == 1) { "Unsupported push entry point" }
    val implementation = requireNotNull(method.implementation) { "Unsupported push entry point" }
    // .locals 1 plus this and the Intent parameter.
    require(implementation.registerCount == 3) { "Unsupported push entry point registers" }
    val code = implementation.instructions.toList()
    require(code.map { it.opcode to operandText(it) } == PRISTINE_HANDLE_INTENT) {
        "Unsupported or already modified push entry point"
    }
}

/**
 * Drops a push before the Firebase SDK can show it or the push plugin can handle it when the
 * intent's `FeatureName` extra marks it as a platform message. Everything else continues to the
 * original body unchanged.
 */
internal fun applyKingnetPushFilter(method: MutableMethod) {
    requireFcmHandleIntentShape(method)
    val original = method.implementation!!.instructions.first()
    method.addInstructionsWithLabels(0, """
        const-string v0, "FeatureName"
        invoke-virtual {p1, v0}, $INTENT->getStringExtra(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v0
        invoke-static {v0}, $KINGNET_PUSH_FILTER->isPlatformNews(Ljava/lang/String;)Z
        move-result v0
        if-eqz v0, :keep_push
        return-void
    """, ExternalLabel("keep_push", original))
}
