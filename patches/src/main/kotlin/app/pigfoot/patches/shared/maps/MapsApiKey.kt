package app.pigfoot.patches.shared.maps

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.security.MessageDigest

internal fun isMapsKey(value: String?) = value.isNullOrBlank() || Regex("AIza[A-Za-z0-9_-]{35}").matches(value.trim())

/** Changes only an existing, uniquely qualified application metadata value.
 * App adapters own original-value pins. Never log/store the supplied value here. */
internal fun replaceMapsApiKey(document: Document, expectedValueSha256: String, candidate: String?): Boolean {
    require(isMapsKey(candidate)) { "Invalid Google Maps API key format" }
    if(candidate.isNullOrBlank()) return false
    val apps=document.documentElement.getElementsByTagName("application")
    require(document.documentElement.tagName=="manifest" && apps.length==1) { "Missing or ambiguous application" }
    val app=apps.item(0) as Element
    fun attr(e: Element,name: String) = e.getAttributeNode("android:$name")
        ?: e.getAttributeNodeNS("http://schemas.android.com/apk/res/android",name)
    val matches=(0 until app.childNodes.length).mapNotNull { app.childNodes.item(it) as? Element }
        .filter { it.tagName=="meta-data" && attr(it,"name")?.value=="com.google.android.geo.API_KEY" }
    require(matches.size==1) { "Missing or ambiguous Google Maps metadata" }
    val value=requireNotNull(attr(matches.single(),"value")) { "Missing Google Maps literal value" }
    val digest=MessageDigest.getInstance("SHA-256").digest(value.value.toByteArray()).joinToString("") { "%02x".format(it) }
    require(expectedValueSha256.matches(Regex("[0-9a-f]{64}")) && digest==expectedValueSha256) { "Unsupported original Google Maps metadata" }
    val replacement=candidate.trim()
    if(value.value==replacement)return false
    value.value=replacement
    return true
}
