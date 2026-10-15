package app.pigfoot.patches.camscanner

import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11n
import com.android.tools.smali.dexlib2.immutable.instruction.ImmutableInstruction11x

internal const val PIRATE_CONTROL = "Lcom/intsig/camscanner/business/PirateAppControl;"
internal const val PIRATE_GATE_DESCRIPTOR = "(Landroid/content/Context;Lcom/intsig/callback/DialogDismissListener;)Z"

/** Replace the whole qualified local prompt gate, not a prefix on the old body. */
internal fun startupGateReplacement(method: Method): ImmutableMethod {
    require(method.definingClass == PIRATE_CONTROL && method.name == "O8" &&
        method.signature() == PIRATE_GATE_DESCRIPTOR && AccessFlags.STATIC.isSet(method.accessFlags)) {
        "Unsupported CamScanner startup gate"
    }
    val impl = requireNotNull(method.implementation)
    require(impl.registerCount == 6 && impl.tryBlocks.isEmpty()) { "Unsupported CamScanner startup gate body" }
    return ImmutableMethod(method.definingClass, method.name, method.parameters, method.returnType,
        method.accessFlags, method.annotations, method.hiddenApiRestrictions,
        ImmutableMethodImplementation(impl.registerCount, listOf(
            ImmutableInstruction11n(Opcode.CONST_4, 0, 0),
            ImmutableInstruction11x(Opcode.RETURN, 0)
        ), emptyList(), emptyList()))
}
