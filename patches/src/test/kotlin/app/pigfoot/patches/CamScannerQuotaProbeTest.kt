package app.pigfoot.patches

import app.pigfoot.patches.camscanner.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import org.junit.Assert.*
import org.junit.Test

class CamScannerQuotaProbeTest {
    private fun method(registers: Int = 11): MutableMethod = MutableMethod(ImmutableMethod(
        QUOTA_CLIENT, "onPageFinished", listOf("Landroid/webkit/WebView;", "Ljava/lang/String;")
            .map { ImmutableMethodParameter(it, emptySet(), null) }, "V", AccessFlags.PUBLIC.value,
        emptySet(), emptySet(), ImmutableMethodImplementation(registers,
            listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList())))

    @Test fun assembledPrefixPreservesArgumentsAndEvaluatesExactScriptOnlyOnScopedHosts() {
        for (failure in listOf<String?>(null, "CONTROL:VerifyError", "CALLBACK:VerifyError", "CONTROL:NoClassDefFoundError", "CALLBACK:IllegalAccessError", "CONTROL:SecurityException", "CALLBACK:RuntimeException")) for (silent in listOf(false, true)) for ((url, hasView, expected) in listOf(
            Triple("https://mo.camscanner.com/lottery/relottery?intsig_key=secret", true, 3),
            Triple("https://www.camscanner.com/lottery/relottery", true, 3),
            Triple("https://mo.camscanner.com.evil.invalid/lottery/relottery", true, 0),
            Triple("http://mo.camscanner.com/lottery/relottery", true, 0),
            Triple(null, true, 0), Triple("https://mo.camscanner.com/lottery/relottery", false, 0))) {
            val m = method(); requireQuotaProbeMethod(m); if (silent) m.addInstructions(0, lotteryInitPrefix()) else installQuotaProbe(m)
            val code = m.implementation!!.instructions.toList()
            val addresses = mutableMapOf<Int, Int>(); var size = 0
            code.forEachIndexed { index, i -> addresses[size] = index; size += i.codeUnits }
            val registers = arrayOfNulls<Any>(11)
            val context = Any(); val toast = Any(); val messages = mutableListOf<String>()
            val control = Any(); val view = if (hasView) Any() else null
            registers[8] = control; registers[9] = view; registers[10] = url
            var pc = 0; var index = 0; var result: Any? = null; var evaluated = 0; var steps = 0; var pending: String? = null; var reflecting = ""
            while (index < code.size) {
                check(++steps < 150)
                val i = code[index]; var next = index + 1
                val one = i as? OneRegisterInstruction
                when(i.opcode) {
                    Opcode.NOP -> Unit
                    Opcode.CONST_STRING -> registers[one!!.registerA] = ((i as ReferenceInstruction).reference as StringReference).string
                    Opcode.CONST_4 -> registers[one!!.registerA] = (i as WideLiteralInstruction).wideLiteral.toInt()
                    Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> registers[one!!.registerA] = result
                    Opcode.IF_EQZ, Opcode.IF_NEZ -> {
                        val value = registers[one!!.registerA]
                        val truth = value != null && value != false && value != 0
                        if (truth == (i.opcode == Opcode.IF_NEZ)) next = addresses.getValue(pc + (i as OffsetInstruction).codeOffset)
                    }
                    Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_STATIC -> {
                        val call = i as FiveRegisterInstruction
                        val ref = (i as ReferenceInstruction).reference.toString()
                        when (ref) {
                            "Ljava/lang/String;->startsWith(Ljava/lang/String;)Z" -> result = (registers[call.registerC] as String).startsWith(registers[call.registerD] as String)
                            "Landroid/content/Context;->getClassLoader()Ljava/lang/ClassLoader;" -> { assertSame(context, registers[call.registerC]); result = "APP_LOADER" }
                            "Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;" -> {
                                assertEquals(0, registers[call.registerD]); assertEquals("APP_LOADER", registers[call.registerE])
                                val name = registers[call.registerC] as String
                                reflecting = if (name == "com.intsig.camscanner.attention.WatchAdControl") "CONTROL" else "CALLBACK"
                                assertEquals(if (reflecting == "CONTROL") WATCH_CONTROL else WATCH_CALLBACK, "L" + name.replace('.', '/') + ";")
                                result = name
                            }
                            "Ljava/lang/Class;->getDeclaredMethods()[Ljava/lang/reflect/Method;" -> {
                                if (failure?.substringBefore(':') == reflecting) {
                                    pending = failure.substringAfter(':')
                                    val block = m.implementation!!.tryBlocks.single { pc >= it.startCodeAddress && pc < it.startCodeAddress + it.codeUnitCount }
                                    assertEquals("Ljava/lang/Throwable;", block.exceptionHandlers.single().exceptionType)
                                    next = addresses.getValue(block.exceptionHandlers.single().handlerCodeAddress)
                                } else result = emptyArray<Any>()
                            }
                            "Ljava/lang/String;->concat(Ljava/lang/String;)Ljava/lang/String;" -> result = (registers[call.registerC] as String) + (registers[call.registerD] as String)
                            "Landroid/view/View;->getContext()Landroid/content/Context;" -> { assertSame(view, registers[call.registerC]); result = context }
                            "Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;" -> {
                                assertSame(context, registers[call.registerC]); assertEquals(1, registers[call.registerE])
                                messages += registers[call.registerD] as String; result = toast
                            }
                            "Landroid/widget/Toast;->show()V" -> assertSame(toast, registers[call.registerC])
                            "Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V" -> {
                                assertSame(view, registers[call.registerC])
                                val check = if (failure == null) "BOTH_RESOLVED" else failure.substringBefore(':') + "_" + when(failure.substringAfter(':')) {
                                    "VerifyError" -> "VERIFY_ERROR"; "NoClassDefFoundError" -> "CLASS_MISSING"
                                    "IllegalAccessError" -> "LINKAGE_ERROR"; "SecurityException" -> "SECURITY_ERROR"; else -> "OTHER_ERROR"
                                }
                                assertEquals(if (silent) lotteryInitOnlyScript else when(evaluated) {
                                    0 -> "window.__csRewardClassCheck='$check';"; 1 -> quotaBootstrapScript; else -> quotaProbeScript
                                }, registers[call.registerD])
                                assertEquals(0, registers[call.registerE]); evaluated++
                            }
                            else -> error("Unexpected native probe call $ref")
                        }
                    }
                    Opcode.GOTO, Opcode.GOTO_16 -> next = addresses.getValue(pc + (i as OffsetInstruction).codeOffset)
                    Opcode.MOVE_EXCEPTION -> registers[one!!.registerA] = pending
                    Opcode.INSTANCE_OF -> {
                        val v = i as TwoRegisterInstruction
                        val type = (i as ReferenceInstruction).reference.toString().substringAfterLast('/').removeSuffix(";")
                        val kind = registers[v.registerB] as String?
                        registers[v.registerA] = kind == type || type == "LinkageError" && kind in setOf("VerifyError", "NoClassDefFoundError", "IllegalAccessError")
                    }
                    Opcode.RETURN_VOID -> break
                    else -> error("Unexpected quota probe opcode ${i.opcode}")
                }
                index = next; pc = addresses.entries.single { it.value == index }.key
            }
            if (!silent) {
                val block = m.implementation!!.tryBlocks.single()
                val guarded = code.indices.filter { val address = addresses.entries.single { e -> e.value == it }.key; address >= block.startCodeAddress && address < block.startCodeAddress + block.codeUnitCount }.map { code[it] }
                assertEquals(2, guarded.count { (it as? ReferenceInstruction)?.reference.toString() == "Ljava/lang/Class;->forName(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;" })
                assertTrue(guarded.none { (it as? ReferenceInstruction)?.reference.toString().contains("evaluateJavascript") })
            }
            assertEquals(if (silent && expected == 3) 1 else expected, evaluated)
            assertEquals(if (silent || !hasView) emptyList<String>() else listOf(when {
                url == null -> "CS-Q23:HOOK NO_PAGE_URL"
                expected == 3 -> "CS-Q23:HOOK SCOPED"
                else -> "CS-Q23:HOOK HOST_OUT_OF_SCOPE"
            }), messages)
            assertTrue(messages.none { "secret" in it || "https://" in it })
            assertSame(control, registers[8]); assertSame(view, registers[9]); assertEquals(url, registers[10])
        }
    }
    @Test fun methodShapeAndUnqualifiedWriterDigestsFailClosed() {
        assertThrows(IllegalArgumentException::class.java) { requireQuotaProbeMethod(method(10)) }
        assertTrue(matchesCamScannerClassPin(QUOTA_CLIENT_PIN, "e5ff5a7b1762abaf68ec714da5e1932940881164cc4d55ae17e25ab526b9238a"))
        assertFalse(matchesCamScannerClassPin(QUOTA_CLIENT_PIN, "e5ff5a7b1762abaf68ec714da5e1932940881164cc4d55ae17e25ab526b9238b"))
    }
}
