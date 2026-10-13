package app.pigfoot.patches.camscanner

internal data class ScannerEdit(val hash: String, val offset: Int, val before: ByteArray, val after: ByteArray)
private fun hex(s: String) = s.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
internal val scannerEdits = mapOf(
    "lib/arm64-v8a/libscannercs.so" to ScannerEdit("f4f696e58cf4d8016cdc7c804f58847fe872edfb0bac9cd1037b4c8552e0361c", 0x9f590, hex("ff0306d1e82f00fd"), hex("000080d2c0035fd6")),
    "lib/armeabi/libscannercs.so" to ScannerEdit("596650657a8801d5df3fde91946b8a962c4256dcf40fcf8bce57493c4fa0fbc9", 0x61a78, hex("2de9f04f"), hex("00207047"))
)
internal fun editScanner(bytes: ByteArray, spec: ScannerEdit): ByteArray {
    require(sha256(bytes) == spec.hash) { "Unsupported or already modified scanner library" }
    require(spec.before.size == spec.after.size && spec.offset >= 0 && spec.offset <= bytes.size - spec.before.size)
    require(bytes.copyOfRange(spec.offset, spec.offset + spec.before.size).contentEquals(spec.before)) { "Scanner entry mismatch" }
    return bytes.copyOf().also { spec.after.copyInto(it, spec.offset) }
}
