package app.pigfoot.patches

import app.pigfoot.patches.camscanner.*
import app.morphe.patcher.patch.OptionException
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory

class CamScannerSelectionTest {
    @Test fun wordDiagnosticsRemainIndependentAndOptIn() {
        assertFalse(camscannerWordDiagnosticsPatch.default)
        assertEquals(setOf(camscannerInputPatch), camscannerWordDiagnosticsPatch.dependencies)
        for (patch in listOf(camscannerLocalFeaturesPatch, camscannerAdsPatch, camscannerOfflinePatch)) {
            assertFalse(patch.dependencies.contains(camscannerWordDiagnosticsPatch))
        }
    }
    @Test fun offlinePullsInAdsButNotPremiumOrTelemetry() {
        assertTrue(camscannerOfflinePatch.dependencies.contains(camscannerAdsPatch))
        for (patch in listOf(camscannerLocalFeaturesPatch, camscannerFirebasePatch, camscannerLoggingPatch, camscannerAdsPatch, camscannerOfflinePatch)) assertFalse(patch.default)
        assertFalse(camscannerOfflinePatch.dependencies.contains(camscannerLocalFeaturesPatch))
        assertFalse(camscannerOfflinePatch.dependencies.contains(camscannerFirebasePatch))
        assertFalse(camscannerAdsPatch.dependencies.contains(camscannerOfflinePatch))
        assertTrue(camscannerFirebasePatch.dependencies.contains(camscannerInputPatch))
    }
    @Test fun scopeAndFolderOptionsRejectUnknownPolicies() {
        val scope = camscannerFirebasePatch.options["scope"]
        val limit = camscannerLocalFeaturesPatch.options["folderLimit"]
        try {
            for (s in listOf("all", "analytics")) camscannerFirebasePatch.options["scope"] = s
            assertThrows(OptionException.ValueValidationException::class.java) { camscannerFirebasePatch.options["scope"] = "unknown" }
            assertThrows(OptionException.ValueRequiredException::class.java) { camscannerFirebasePatch.options.set<String>("scope", null) }
            for (n in listOf(0, 5000)) camscannerLocalFeaturesPatch.options["folderLimit"] = n
            assertThrows(OptionException.ValueValidationException::class.java) { camscannerLocalFeaturesPatch.options["folderLimit"] = -1 }
        } finally { scope.reset(); limit.reset() }
    }
    private fun document(xml: String) = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(ByteArrayInputStream(xml.toByteArray()))
    @Test fun offlineRemovesDuplicateInternetWithoutRemovingOtherPermissions() {
        val doc = document("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"><uses-permission android:name="android.permission.INTERNET"/><uses-permission android:name="android.permission.CAMERA"/><uses-permission android:name="android.permission.INTERNET"/><application/></manifest>""")
        assertEquals(2, removeInternet(doc))
        assertEquals(1, doc.getElementsByTagName("uses-permission").length)
        assertEquals("android.permission.CAMERA", (doc.getElementsByTagName("uses-permission").item(0) as org.w3c.dom.Element).getAttribute("android:name"))
        assertThrows(IllegalArgumentException::class.java) { removeInternet(doc) }
    }
    @Test fun adsCleanupPreservesLoginBillingAndWorkManager() {
        val names = listOf("com.vungle.ads.VungleProvider", "com.vk.api.sdk.internal.VKInitProvider", "com.intsig.wxapi.WXEntryActivity", "com.android.billingclient.api.ProxyBillingActivity", "androidx.work.impl.background.systemalarm.ConstraintProxy\$NetworkStateProxy", "com.google.android.gms.measurement.AppMeasurementService")
        val doc = document("<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application>" + names.joinToString("") { "<service android:name=\"$it\"/>" } + "</application></manifest>")
        assertEquals(1, removeAdComponents(doc))
        assertEquals(5, doc.getElementsByTagName("service").length)
    }
    @Test fun metadataUpdatesRatherThanDuplicatingAndRejectsAmbiguity() {
        val doc = document("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><meta-data android:name="firebase_sessions_enabled" android:value="true"/></application></manifest>""")
        metadata(doc, "firebase_sessions_enabled", "false")
        metadata(doc, "firebase_sessions_enabled", "false")
        assertEquals(1, doc.getElementsByTagName("meta-data").length)
        assertEquals("false", (doc.getElementsByTagName("meta-data").item(0) as org.w3c.dom.Element).getAttribute("android:value"))
        val duplicate = document("""<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application><meta-data android:name="x"/><meta-data android:name="x"/></application></manifest>""")
        assertThrows(IllegalArgumentException::class.java) { metadata(duplicate, "x", "false") }
    }
}
