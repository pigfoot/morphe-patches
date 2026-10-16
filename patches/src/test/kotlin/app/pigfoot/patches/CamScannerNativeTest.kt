package app.pigfoot.patches

import app.pigfoot.patches.camscanner.*
import org.junit.Assert.*
import org.junit.Test

class CamScannerNativeTest {
    @Test fun editsOnlyExpectedBytesAndRejectsTamperedOrRepeatedInput() {
        val original = byteArrayOf(7, 9, 1, 2, 3, 11)
        val spec = ScannerEdit(sha256(original), 2, byteArrayOf(1, 2), byteArrayOf(0, 0))
        val result = editScanner(original, spec)
        assertArrayEquals(byteArrayOf(7, 9, 0, 0, 3, 11), result)
        assertArrayEquals(byteArrayOf(7, 9, 1, 2, 3, 11), original)
        assertThrows(IllegalArgumentException::class.java) { editScanner(result, spec) }
        assertThrows(IllegalArgumentException::class.java) { editScanner(original.copyOf().also { it[0] = 0 }, spec) }
        assertThrows(IllegalArgumentException::class.java) { editScanner(original, spec.copy(offset = 100)) }
        assertThrows(IllegalArgumentException::class.java) { editScanner(original, spec.copy(before = byteArrayOf(0, 0))) }
    }
    @Test fun onlyQualifiedScannerPathsCanBeSelected() {
        assertEquals(setOf("lib/arm64-v8a/libscannercs.so", "lib/armeabi/libscannercs.so"), scannerEdits.keys)
        assertFalse(scannerEdits.containsKey("lib/x86/libscannercs.so"))
        assertFalse(camscannerStartupPatch.default)
        assertFalse(camscannerOfflinePatch.dependencies.contains(camscannerStartupPatch))
    }
}
