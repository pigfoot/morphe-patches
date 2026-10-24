package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

/** Explicit diagnosis only: reflection resolution, never instance construction or invocation. */
internal fun nativeRewardClassCheckPrefix(): String {
    val control = WATCH_CONTROL.removeSurrounding("L", ";").replace('/', '.')
    val callback = WATCH_CALLBACK.removeSurrounding("L", ";").replace('/', '.')
    return """
        const-string v5, "window.__csRewardClassCheck='CONTROL_"
        invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;
        move-result-object v0
        invoke-virtual {v0}, Landroid/content/Context;->getClassLoader()Ljava/lang/ClassLoader;
        move-result-object v4
        const/4 v2, 0x0
        const-string v0, "$control"
        invoke-static {v0, v2, v4}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
        move-result-object v1
        invoke-virtual {v1}, Ljava/lang/Class;->getDeclaredMethods()[Ljava/lang/reflect/Method;
        const-string v5, "window.__csRewardClassCheck='CALLBACK_"
        const-string v0, "$callback"
        invoke-static {v0, v2, v4}, Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;
        move-result-object v1
        invoke-virtual {v1}, Ljava/lang/Class;->getDeclaredMethods()[Ljava/lang/reflect/Method;
        const-string v0, "window.__csRewardClassCheck='BOTH_RESOLVED';"
        goto :reward_class_report
        :reward_class_failed
        move-exception v6
        instance-of v0, v6, Ljava/lang/VerifyError;
        if-nez v0, :reward_class_verify
        instance-of v0, v6, Ljava/lang/NoClassDefFoundError;
        if-nez v0, :reward_class_missing
        instance-of v0, v6, Ljava/lang/LinkageError;
        if-nez v0, :reward_class_linkage
        instance-of v0, v6, Ljava/lang/SecurityException;
        if-nez v0, :reward_class_security
        const-string v0, "OTHER_ERROR';"
        goto :reward_class_join
        :reward_class_verify
        const-string v0, "VERIFY_ERROR';"
        goto :reward_class_join
        :reward_class_missing
        const-string v0, "CLASS_MISSING';"
        goto :reward_class_join
        :reward_class_linkage
        const-string v0, "LINKAGE_ERROR';"
        goto :reward_class_join
        :reward_class_security
        const-string v0, "SECURITY_ERROR';"
        :reward_class_join
        invoke-virtual {v5, v0}, Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;
        move-result-object v0
        :reward_class_report
        const/4 v1, 0x0
        invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
    """.trimIndent()
}

/** Catch only the added self-check; never catch an original page method failure. */
internal fun installQuotaProbe(method: MutableMethod) {
    requireQuotaProbeMethod(method)
    method.addInstructions(0, quotaProbePrefix())
    val impl = requireNotNull(method.implementation)
    val code = impl.instructions.toList()
    fun literal(index: Int) = ((code[index] as? ReferenceInstruction)?.reference as? StringReference)?.string
    val start = code.indices.single { literal(it) == "window.__csRewardClassCheck='CONTROL_" }
    val end = code.indices.single { literal(it) == "window.__csRewardClassCheck='BOTH_RESOLVED';" }
    val handler = (end + 1 until code.size).first { code[it].opcode == Opcode.MOVE_EXCEPTION }
    impl.addCatch("Ljava/lang/Throwable;", impl.newLabelForIndex(start), impl.newLabelForIndex(end), impl.newLabelForIndex(handler))
}
