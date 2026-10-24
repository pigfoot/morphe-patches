package app.pigfoot.patches

import app.pigfoot.patches.camscanner.*
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.*
import com.android.tools.smali.dexlib2.analysis.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class CamScannerNativePreloadCaptureTest {
    private fun method(): MutableMethod {
        val m = MutableMethod(ImmutableMethod(NATIVE_BRIDGE, "callApp",
            listOf(ImmutableMethodParameter("Ljava/lang/String;", emptySet(), null)), "V",
            AccessFlags.PUBLIC.value, emptySet(), emptySet(), ImmutableMethodImplementation(5,
                listOf(ImmutableInstruction10x(Opcode.RETURN_VOID)), emptyList(), emptyList())))
        m.addInstructions(0, "invoke-static {p0, p1}, Ltest/Original;->run(Ljava/lang/Object;Ljava/lang/String;)V")
        return m
    }
    private data class Result(val calls: Int, val tags: List<String>, val thrown: Throwable?)
    /** Executes assembled instructions, including actual exception edges; not an ART emulator. */
    private fun execute(raw: String?, originalError: Throwable? = null, diagnosticFailure: String? = null): Result {
        val m = method(); installNativePreloadCapture(m)
        val impl = m.implementation!!; val code = impl.instructions.toList()
        val offsets = mutableListOf<Int>(); var units = 0
        code.forEach { offsets += units; units += it.codeUnits }
        val receiver = Any(); val fragment = Any(); val r = arrayOfNulls<Any>(5)
        r[3] = receiver; r[4] = raw
        var pc = 0; var steps = 0; var result: Any? = null; var pending: Throwable? = null; var calls = 0
        val tags = mutableListOf<String>()
        fun truth(v: Any?) = v != null && v != 0 && v != false
        while (pc < code.size) {
            check(++steps < 700)
            val i = code[pc]; val one = i as? OneRegisterInstruction; val two = i as? TwoRegisterInstruction
            var next = pc + 1
            fun jump() { next = offsets.indexOf(offsets[pc] + (i as OffsetInstruction).codeOffset); check(next >= 0) }
            try {
                when (i.opcode) {
                    Opcode.NOP -> Unit
                    Opcode.CONST_STRING -> r[one!!.registerA] = ((i as ReferenceInstruction).reference as StringReference).string
                    Opcode.CONST_4, Opcode.CONST_16 -> r[one!!.registerA] = (i as WideLiteralInstruction).wideLiteral.toInt()
                    Opcode.NEW_INSTANCE -> r[one!!.registerA] = "NEW_JSON"
                    Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT -> r[one!!.registerA] = result
                    Opcode.MOVE_EXCEPTION -> { r[one!!.registerA] = pending; pending = null }
                    Opcode.IF_EQZ -> if (!truth(r[one!!.registerA])) jump()
                    Opcode.IF_NEZ -> if (truth(r[one!!.registerA])) jump()
                    Opcode.IF_NE -> if (r[two!!.registerA] != r[two.registerB]) jump()
                    Opcode.IF_GT -> if ((r[two!!.registerA] as Int) > (r[two.registerB] as Int)) jump()
                    Opcode.GOTO_16 -> jump()
                    Opcode.ARRAY_LENGTH -> r[two!!.registerA] = (r[two.registerB] as Array<*>).size
                    Opcode.AGET_OBJECT -> { val a = i as ThreeRegisterInstruction; r[a.registerA] = (r[a.registerB] as Array<*>)[r[a.registerC] as Int] }
                    Opcode.IGET_OBJECT -> { assertSame(receiver, r[two!!.registerB]); r[two.registerA] = fragment }
                    Opcode.INVOKE_VIRTUAL, Opcode.INVOKE_DIRECT, Opcode.INVOKE_STATIC -> {
                        val c = i as FiveRegisterInstruction
                        val ref = (i as ReferenceInstruction).reference.toString()
                        val a = r[c.registerC]; val b = r[c.registerD]
                        when (ref) {
                            "Ljava/lang/String;->length()I" -> result = (a as String).length
                            "Ljava/lang/String;->equals(Ljava/lang/Object;)Z" -> result = if (a == b) 1 else 0
                            "Lorg/json/JSONObject;-><init>(Ljava/lang/String;)V" -> {
                                if (diagnosticFailure == "parse") throw IllegalStateException("diagnostic parser")
                                r[c.registerC] = JSONObject(b as String)
                            }
                            "Lorg/json/JSONObject;->optString(Ljava/lang/String;)Ljava/lang/String;" -> result = (a as JSONObject).optString(b as String)
                            "Lorg/json/JSONObject;->optJSONObject(Ljava/lang/String;)Lorg/json/JSONObject;" -> result = (a as JSONObject).optJSONObject(b as String)
                            "Lorg/json/JSONObject;->optInt(Ljava/lang/String;)I" -> result = (a as JSONObject).optInt(b as String)
                            "Ljava/lang/Object;->getClass()Ljava/lang/Class;" -> result = a!!.javaClass
                            "Ljava/lang/Class;->getName()Ljava/lang/String;" -> result = (a as Class<*>).name
                            "Ljava/lang/Throwable;->getStackTrace()[Ljava/lang/StackTraceElement;" -> {
                                if (diagnosticFailure == "stack") throw IllegalStateException("diagnostic stack")
                                result = (a as Throwable).stackTrace
                            }
                            "Ljava/lang/StackTraceElement;->getClassName()Ljava/lang/String;" -> result = (a as StackTraceElement).className
                            "Lcom/intsig/webview/WebViewFragment;->〇0oO(Ljava/lang/String;)V" -> {
                                assertSame(fragment, a)
                                if (diagnosticFailure == "post") throw IllegalStateException("diagnostic post")
                                val packet = JSONObject(b as String); assertEquals(setOf("__cs_native_preload_v25"), packet.keySet())
                                tags += packet.getString("__cs_native_preload_v25")
                            }
                            "Ltest/Original;->run(Ljava/lang/Object;Ljava/lang/String;)V" -> {
                                calls++; assertSame(receiver, a); assertSame(raw, b)
                                if (originalError != null) throw originalError
                            }
                            else -> error("Unexpected native call $ref")
                        }
                    }
                    Opcode.THROW -> return Result(calls, tags, r[one!!.registerA] as Throwable)
                    Opcode.RETURN_VOID -> return Result(calls, tags, null)
                    else -> error("Unexpected native opcode ${i.opcode}")
                }
            } catch (e: Throwable) {
                val block = impl.tryBlocks.singleOrNull { offsets[pc] >= it.startCodeAddress && offsets[pc] < it.startCodeAddress + it.codeUnitCount }
                if (block == null) throw e
                assertEquals("Ljava/lang/Throwable;", block.exceptionHandlers.single().exceptionType)
                pending = e; next = offsets.indexOf(block.exceptionHandlers.single().handlerCodeAddress); check(next >= 0)
            }
            pc = next
        }
        error("Fell through native body")
    }
    private val preload = """{"action":"watchad_lottery","data":{"prestrain":1}}"""
    @Test fun originalOutcomesAndThrowableIdentitySurviveDiagnosticFailures() {
        for (failure in listOf(null, "parse", "post", "stack")) for (error in listOf(null, NullPointerException("private"), AssertionError("private"))) {
            error?.stackTrace = arrayOf(StackTraceElement("com.intsig.camscanner.attention.WatchAdControl", "private", "private", 42))
            val r = execute(preload, error, failure)
            assertEquals(1, r.calls); assertSame(error, r.thrown)
            if (failure == null) assertEquals(if (error == null) listOf("ENTERED") else listOf("ENTERED", if (error is NullPointerException) "THREW_NULL_POINTER" else "THREW_OTHER", "SITE_WATCH_CONTROL"), r.tags)
            if (failure in listOf("parse", "post")) assertTrue(r.tags.isEmpty())
            assertTrue(r.tags.none { "private" in it })
        }
    }
    @Test fun nonPreloadMalformedAndOversizeInputsKeepOriginalSemantics() {
        for (raw in listOf(null, "bad", "x".repeat(8193), "{}", """{"action":"getGift","data":{"prestrain":1}}""", """{"action":"watchad_lottery","data":{"prestrain":0}}""")) {
            for (error in listOf(null, IllegalArgumentException("private"))) {
                val r = execute(raw, error); assertEquals(1, r.calls); assertSame(error, r.thrown); assertTrue(r.tags.isEmpty())
            }
        }
    }
    @Test fun throwableRegisterIsDefinitelyInitializedAndOriginalArgumentsStayParameters() {
        val m = method(); installNativePreloadCapture(m)
        val path = ClassPath(ClassProvider { type ->
            if (type == NATIVE_BRIDGE || type.startsWith("Ltest/") || type.startsWith("Lorg/json/") || type.startsWith("Lcom/intsig/"))
                ImmutableClassDef(type, AccessFlags.PUBLIC.value, "Ljava/lang/Object;", emptyList(), null, emptySet(), emptyList(), if (type == NATIVE_BRIDGE) listOf(m) else emptyList()) else null
        })
        val analyzer = MethodAnalyzer(path, m, null, false); assertNull(analyzer.analysisException)
        val throwing = analyzer.analyzedInstructions.single { it.instruction.opcode == Opcode.THROW }
        val caught = throwing.getPreInstructionRegisterType(2)
        assertEquals(RegisterType.REFERENCE, caught.category); assertEquals("Ljava/lang/Throwable;", caught.type?.type)
        assertEquals(5, m.implementation!!.registerCount)
        assertThrows(IllegalArgumentException::class.java) { installNativePreloadCapture(m) }
    }
}
