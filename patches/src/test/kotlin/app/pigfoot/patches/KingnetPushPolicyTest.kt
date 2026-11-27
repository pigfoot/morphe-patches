package app.pigfoot.patches

import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.pigfoot.patches.kingnet.*
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.immutable.reference.*
import org.junit.Assert.*
import org.junit.Test

class KingnetPushPolicyTest {
    private val intent = "Landroid/content/Intent;"
    private fun str(register: Int, text: String) =
        ImmutableInstruction21c(Opcode.CONST_STRING, register, ImmutableStringReference(text))
    private fun remove() = ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 2, 2, 0, 0, 0, 0,
        ImmutableMethodReference(intent, "removeExtra", listOf("Ljava/lang/String;"), "V"))

    private fun pristineCode() = listOf(
        str(0, "intent"),
        ImmutableInstruction35c(Opcode.INVOKE_STATIC, 2, 2, 0, 0, 0, 0,
            ImmutableMethodReference("Lkotlin/jvm/internal/Intrinsics;", "checkNotNullParameter",
                listOf("Ljava/lang/Object;", "Ljava/lang/String;"), "V")),
        str(0, "gcm.n.event_time"), remove(),
        str(0, "gcm.notification.event_time"), remove(),
        ImmutableInstruction35c(Opcode.INVOKE_SUPER, 2, 1, 2, 0, 0, 0,
            ImmutableMethodReference("Lcom/google/firebase/messaging/FirebaseMessagingService;", "handleIntent",
                listOf(intent), "V")),
        ImmutableInstruction10x(Opcode.RETURN_VOID))

    private fun method(owner: String = "Lcom/adobe/phonegap/push/FCMService;", name: String = "handleIntent",
                       parameter: String = intent, flags: Int = 1, registers: Int = 3,
                       code: List<ImmutableInstruction> = pristineCode()) =
        MutableMethod(ImmutableMethod(owner, name, listOf(ImmutableMethodParameter(parameter, emptySet(), null)),
            "V", flags, emptySet(), emptySet(), ImmutableMethodImplementation(registers, code, emptyList(), emptyList())))

    @Test fun pristineEntryPointIsAcceptedAndOnlyGainsTheGuardInFront() {
        val target = method()
        val before = target.implementation!!.instructions.toList()
        applyKingnetPushFilter(target)
        val after = target.implementation!!.instructions.toList()
        assertEquals(before.size + 7, after.size)
        // The original body follows the guard unchanged.
        assertEquals(before.map { it.opcode to (it as? ReferenceInstruction)?.reference?.toString() },
            after.drop(7).map { it.opcode to (it as? ReferenceInstruction)?.reference?.toString() })
        assertEquals(listOf(Opcode.CONST_STRING, Opcode.INVOKE_VIRTUAL, Opcode.MOVE_RESULT_OBJECT, Opcode.INVOKE_STATIC,
            Opcode.MOVE_RESULT, Opcode.IF_EQZ, Opcode.RETURN_VOID), after.take(7).map { it.opcode })
        val refs = after.take(4).filterIsInstance<ReferenceInstruction>().map { it.reference.toString() }
        assertTrue(refs.any { it.contains("getStringExtra") })
        assertTrue(refs.any { it.contains("Lapp/pigfoot/kingnet/PushFilter;->isPlatformNews(Ljava/lang/String;)Z") })
    }

    @Test fun anythingButThePristineEntryPointRejectsWithoutMutation() {
        val wrongOperand = pristineCode().toMutableList().also { it[2] = str(0, "gcm.n.event_time2") }
        val missingSuper = pristineCode().filterIndexed { index, _ -> index != 6 }
        val extra = pristineCode() + ImmutableInstruction10x(Opcode.NOP)
        for (candidate in listOf(
            method(owner = "Lcom/adobe/phonegap/push/Other;"), method(name = "onMessageReceived"),
            method(parameter = "Landroid/os/Bundle;"), method(flags = 17), method(registers = 4),
            method(code = wrongOperand), method(code = missingSuper), method(code = extra))) {
            val before = candidate.implementation!!.instructions.toList()
            assertThrows(IllegalArgumentException::class.java) { applyKingnetPushFilter(candidate) }
            assertEquals(before, candidate.implementation!!.instructions.toList())
        }
    }

    @Test fun alreadyPatchedEntryPointIsRejected() {
        val target = method()
        applyKingnetPushFilter(target)
        val once = target.implementation!!.instructions.toList()
        assertThrows(IllegalArgumentException::class.java) { applyKingnetPushFilter(target) }
        assertEquals(once, target.implementation!!.instructions.toList())
    }
}
