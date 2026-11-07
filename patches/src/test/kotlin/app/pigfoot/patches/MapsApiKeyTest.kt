package app.pigfoot.patches

import app.pigfoot.patches.shared.maps.*
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.*
import org.junit.Test

class MapsApiKeyTest {
    private val old="AIza"+"A".repeat(35)
    private val replacement="AIza"+"B".repeat(35)
    private val hash=MessageDigest.getInstance("SHA-256").digest(old.toByteArray()).joinToString(""){"%02x".format(it)}
    private fun doc(entries: String = "<meta-data android:name='com.google.android.geo.API_KEY' android:value='$old'/>") =
        DocumentBuilderFactory.newInstance().also { it.isNamespaceAware=true }.newDocumentBuilder()
            .parse(ByteArrayInputStream("<manifest xmlns:android='http://schemas.android.com/apk/res/android' package='unchanged'><application android:label='unchanged'>$entries<meta-data android:name='other' android:value='untouched'/></application></manifest>".toByteArray()))
    private fun xml(d: org.w3c.dom.Document): String {
        val w=java.io.StringWriter()
        javax.xml.transform.TransformerFactory.newInstance().newTransformer().transform(
            javax.xml.transform.dom.DOMSource(d),javax.xml.transform.stream.StreamResult(w))
        return w.toString()
    }
    @Test fun optionalValueOnlyChangeKeepsIdentityAndUnrelatedMetadata() {
        val d=doc();assertFalse(replaceMapsApiKey(d,hash,""));assertTrue(replaceMapsApiKey(d,hash," $replacement "))
        assertEquals("unchanged",d.documentElement.getAttribute("package"))
        val metas=d.getElementsByTagName("meta-data")
        assertEquals(replacement,metas.item(0).attributes.getNamedItem("android:value").nodeValue)
        assertEquals("untouched",metas.item(1).attributes.getNamedItem("android:value").nodeValue)
    }
    @Test fun malformedMissingDuplicateOrModifiedOriginalRejectBeforeMutation() {
        assertFalse(isMapsKey("not-a-key"));assertTrue(isMapsKey(replacement))
        val entry="<meta-data android:name='com.google.android.geo.API_KEY' android:value='$old'/>"
        for(d in listOf(doc(""),doc(entry+entry),doc(entry.replace(old,replacement)))) {
            val before=xml(d)
            assertThrows(IllegalArgumentException::class.java){replaceMapsApiKey(d,hash,replacement)}
            assertEquals(before,xml(d))
        }
        val d=doc();assertThrows(IllegalArgumentException::class.java){replaceMapsApiKey(d,hash,"bad")}
        assertEquals(old,d.getElementsByTagName("meta-data").item(0).attributes.getNamedItem("android:value").nodeValue)
    }
}
