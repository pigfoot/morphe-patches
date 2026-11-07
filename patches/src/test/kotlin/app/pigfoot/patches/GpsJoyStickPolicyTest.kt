package app.pigfoot.patches

import app.pigfoot.patches.gpsjoystick.*
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.immutable.*
import com.android.tools.smali.dexlib2.immutable.instruction.*
import com.android.tools.smali.dexlib2.iface.instruction.*
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import org.junit.Assert.*
import org.junit.Test

class GpsJoyStickPolicyTest {
    private fun method(owner: String,name: String,params: List<String>,returns: String,registers: Int) = MutableMethod(ImmutableMethod(
        owner,name,params.map { ImmutableMethodParameter(it,emptySet(),null) },returns,1,emptySet(),emptySet(),
        ImmutableMethodImplementation(registers,listOf(ImmutableInstruction11n(Opcode.CONST_4,0,0),if(returns=="V") ImmutableInstruction10x(Opcode.RETURN_VOID) else ImmutableInstruction11x(Opcode.RETURN_OBJECT,registers-1)),emptyList(),emptyList())))

    @Test fun emittedPreferenceBranchesRestrictOverrideToTwoExactEnumKeys() {
        for(policy in listOf(gps533,gps534)) {
        val m=method(policy.prefs,policy.preferenceMethod,listOf(policy.keys,"Ljava/lang/Object;"),"Ljava/lang/Object;",6)
        enableGpsLocalEntitlements(m,policy)
        val code=m.implementation!!.instructions.toList();val at=mutableMapOf<Int,Int>();var address=0
        for((i,x) in code.withIndex()){at[address]=i;address+=x.codeUnits}
        fun run(key: String?): String {
            val regs=mutableMapOf<Int,String?>(4 to key,5 to "untouched-default");var pc=0;var budget=20
            while(budget-->0){
                // Original first instruction must be reached for every non-target key.
                if(pc==6){assertEquals(key,regs[4]);assertEquals("untouched-default",regs[5]);return "original"}
                val x=code[pc];val here=code.take(pc).sumOf { it.codeUnits }
                when(x.opcode){
                    Opcode.SGET_OBJECT -> { val f=(x as ReferenceInstruction).reference as FieldReference
                        assertTrue(f.definingClass==policy.keys || (f.definingClass=="Ljava/lang/Boolean;" && f.name=="TRUE"))
                        regs[(x as OneRegisterInstruction).registerA]=f.name;pc++ }
                    Opcode.IF_EQ,Opcode.IF_NE -> { val v=x as TwoRegisterInstruction
                        val equal=regs[v.registerA]==regs[v.registerB]
                        pc=if(equal==(x.opcode==Opcode.IF_EQ)) at[here+(x as OffsetInstruction).codeOffset]!! else pc+1 }
                    Opcode.RETURN_OBJECT -> return regs[(x as OneRegisterInstruction).registerA]!!
                    else -> fail("Unexpected reachable opcode ${x.opcode}")
                }
            };error("loop")
        }
        assertEquals("TRUE",run(policy.proField));assertEquals("TRUE",run(policy.adFreeField))
        for(key in listOf(null,"unrelated","map_key","overlay_permission")+listOf("P0","E0","O0","D0").filter { it!=policy.proField && it!=policy.adFreeField })assertEquals("original",run(key))
        assertEquals(8,code.size)
        }
    }

    @Test fun localRestoreNormalizesOnlyStatusParameterAndRetainsOriginalBody() {
        val m=method(GPS_RESTORE,"e",listOf("I"),"V",8);val before=m.implementation!!.instructions.toList()
        keepGpsPremiumOnRestore(m);val after=m.implementation!!.instructions.toList()
        assertEquals(Opcode.CONST_4,after[0].opcode)
        assertEquals(7,(after[0] as OneRegisterInstruction).registerA)
        assertEquals(3,(after[0] as NarrowLiteralInstruction).narrowLiteral)
        assertEquals(before.map { it.opcode },after.drop(1).map { it.opcode })
        assertTrue(m.implementation!!.tryBlocks.isEmpty())
    }

    @Test fun unsupportedTargetOrRegisterShapeFailsWithoutMutation() {
        requireGpsTarget(GPS_PACKAGE,"5.3.3","3030")
        requireGpsTarget(GPS_PACKAGE,"5.3.4","3031")
        assertSame(gps533,gpsPolicyFor(GPS_PACKAGE,"5.3.3","3030"))
        assertSame(gps534,gpsPolicyFor(GPS_PACKAGE,"5.3.4","3031"))
        assertThrows(IllegalArgumentException::class.java){requireGpsTarget(GPS_PACKAGE,"5.3.4","3030")}
        for((p,v,c) in listOf(Triple("other","5.3.3","3030"),Triple(GPS_PACKAGE,"5.3.2","3030"),Triple(GPS_PACKAGE,"5.3.3","3031")))
            assertThrows(IllegalArgumentException::class.java){requireGpsTarget(p,v,c)}
        val m=method(GPS_PREFS,"b",listOf(GPS_KEYS,"Ljava/lang/Object;"),"Ljava/lang/Object;",5)
        val before=m.implementation!!.instructions.toList()
        assertThrows(IllegalArgumentException::class.java){enableGpsLocalEntitlements(m)}
        assertEquals(before,m.implementation!!.instructions.toList())
    }
}
