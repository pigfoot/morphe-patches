package app.pigfoot.patches.camscanner

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.*
import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import com.android.tools.smali.dexlib2.writer.pool.DexPool
import com.android.tools.smali.dexlib2.writer.io.MemoryDataStore
import java.security.MessageDigest

internal const val CAM_PACKAGE = "com.intsig.camscanner"
internal const val CAM_VERSION = "7.26.5.2609160000"
private const val ACCOUNT = "Lcom/intsig/comm/account_data/AccountPreference;"
private const val FABRIC = "Lcom/intsig/crashapm/log/FabricUtils;"
private const val ANALYTICS = "Lcom/google/firebase/analytics/FirebaseAnalytics;"
private const val CRASH = "Lcom/google/firebase/crashlytics/FirebaseCrashlytics;"
private const val LOG = "Lcom/intsig/logagent/LogAgent;"
private val target = Compatibility(packageName = CAM_PACKAGE, name = "CamScanner", apkFileType = ApkFileType.APKM,
    targets = listOf(AppTarget(CAM_VERSION, versionCodes = mapOf(SupportedAbi.ARM64_V8A to 72651,
        SupportedAbi.ARMEABI_V7A to 72651), isExperimental = true)))

internal fun Method.signature() = "(" + parameterTypes.joinToString("") + ")" + returnType
internal fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

// One dependency validates pristine definitions before shared classes are mutated by any selected feature.
internal val camscannerInputPatch = bytecodePatch {
    execute {
        require(packageMetadata.packageName == CAM_PACKAGE && packageMetadata.versionName == CAM_VERSION &&
            packageMetadata.versionCode == "72651") { "Unsupported CamScanner input" }
        require(classDefByOrNull("Lbin/ghost/yrf;") == null) { "Already modified CamScanner input" }
        for ((type, expected) in originalClassPins) {
            val pool = DexPool(Opcodes.forDexVersion(39))
            pool.internClass(classDefBy(type))
            val store = MemoryDataStore()
            pool.writeTo(store)
            require(sha256(store.data) == expected) { "Unsupported or already modified class: $type" }
        }
    }
}
private fun BytecodePatchContext.stop(type: String, name: String, descriptor: String, code: String = "return-void") {
    val method = mutableClassDefBy(type).methods.single { it.name == name && it.signature() == descriptor }
    requireNotNull(method.implementation)
    method.addInstructions(0, code)
}
private fun noNetwork(installer: InstallerType) = if (installer == InstallerType.MOUNT)
    PatchAvailability.UNAVAILABLE else PatchAvailability.DISABLED

val camscannerLocalFeaturesPatch = bytecodePatch(
    name = "CamScanner local premium features", description = "Experimental local account-status features. Does not unlock server credits or cloud quotas.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch)
    val folderLimit by intOption(key = "folderLimit", default = 0, title = "Folder limit",
        values = mapOf("Keep original limit" to 0, "5000 folders" to 5000), required = true,
        validator = { it == 0 || it == 5000 })
    execute {
        val status = mutableClassDefBy(ACCOUNT).methods.single { m -> m.returnType == "J" &&
            m.implementation?.instructions?.any { ((it as? ReferenceInstruction)?.reference as? StringReference)?.string == "qp3sdjd79xhdas02sd" } == true }
        require(status.implementation!!.registerCount >= 2)
        status.addInstructions(0, "const-wide/16 v0, 1\nreturn-wide v0")
        for (name in listOf("Oo8Oo00oo", "〇0000OOO", "o〇0OOo〇0")) stop(ACCOUNT, name, "()Z", "const/4 v0, 1\nreturn v0")
        stop("Lcom/intsig/camscanner/tsapp/sync/SyncUtil;", "o88O8", "()Z", "const/4 v0, 1\nreturn v0")
        if (folderLimit == 5000) stop("Lcom/intsig/camscanner/util/PreferenceHelper;", "o〇00O", "()I", "const/16 v0, 5000\nreturn v0")
    }
}

private val firebaseScope = stringOption(key = "scope", default = "all", title = "Telemetry scope", required = true,
    values = mapOf("All Firebase telemetry" to "all", "Analytics only" to "analytics"),
    validator = { it in setOf("all", "analytics") })
private val firebaseManifestPatch = resourcePatch {
    dependsOn(camscannerInputPatch)
    execute {
        document("AndroidManifest.xml").use { doc ->
            metadata(doc, "firebase_analytics_collection_deactivated", "true")
            metadata(doc, "firebase_analytics_collection_enabled", "false")
            if (firebaseScope.value == "all") {
                metadata(doc, "firebase_crashlytics_collection_enabled", "false")
                metadata(doc, "firebase_performance_collection_deactivated", "true")
                metadata(doc, "firebase_sessions_enabled", "false")
            }
        }
    }
}
val camscannerFirebasePatch = bytecodePatch(
    name = "CamScanner disable Firebase telemetry", description = "Experimental Analytics or Analytics/Crashlytics/Performance/Sessions suppression. Preserve unrelated Firebase functionality.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch, firebaseManifestPatch)
    val scope by firebaseScope()
    execute {
        stop(ANALYTICS, "〇o〇", "(Z)V")
        stop(ANALYTICS, "〇o00〇〇Oo", "(Ljava/lang/String;Landroid/os/Bundle;)V")
        val all = scope == "all"
        val candidates = mutableClassDefBy(FABRIC).methods.filter { it.name != "<init>" && it.name != "<clinit>" &&
            it.name in setOf("O8", "OO0o〇〇〇〇0", "Oo08", "oO80", "o〇0", "〇080", "〇80〇808〇O", "〇o00〇〇Oo", "〇o〇", "〇〇888") }
        require(candidates.size == 10)
        for (m in candidates) {
            if (!all && m.name in setOf("〇o〇", "〇80〇808〇O")) continue
            m.addInstructions(0, if (m.returnType == "Z") "const/4 v0, 0\nreturn v0" else "return-void")
        }
        if (all) {
            // Persisted SDK overrides can win over manifest defaults. Enforce the arbiter too.
            val gates = mutableClassDefBy("Lcom/google/firebase/crashlytics/internal/common/DataCollectionArbiter;")
                .methods.filter { it.returnType == "Z" && it.parameterTypes.isEmpty() }
            require(gates.size == 2)
            gates.forEach { it.addInstructions(0, "const/4 v0, 0\nreturn v0") }
            for (m in mutableClassDefBy(CRASH).methods.filter { it.name in setOf("log", "recordException", "setUserId") && it.returnType == "V" }) m.addInstructions(0, "return-void")
            stop(CRASH, "setCrashlyticsCollectionEnabled", "(Z)V", "const/4 p1, 0")
            stop(CRASH, "setCrashlyticsCollectionEnabled", "(Ljava/lang/Boolean;)V", "sget-object p1, Ljava/lang/Boolean;->FALSE:Ljava/lang/Boolean;")
            stop(CRASH, "sendUnsentReports", "()V")
            // Permanent deactivation is manifest-controlled; prevent later public API overrides too.
            val perf = mutableClassDefBy("Lcom/google/firebase/perf/FirebasePerformance;").methods
                .filter { it.name in setOf("o〇0", "〇〇888") && it.returnType == "V" }
            require(perf.size == 2)
            perf.forEach { it.addInstructions(0, "return-void") }
        }
    }
}

val camscannerLoggingPatch = bytecodePatch(
    name = "CamScanner disable app logging", description = "Experimental network logger/replay suppression with optional local Logcat/file output suppression. Not every third-party SDK log.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch)
    val local by booleanOption(key = "localOutput", default = true, title = "Disable local app logs", required = true)
    execute {
        stop("Lcom/intsig/log/LogAgentHelper;", "oO", "()Z", "const/4 v0, 1\nreturn v0")
        for (m in mutableClassDefBy(LOG).methods.filter { it.returnType == "V" && it.name in setOf("record", "dispatchLogData", "readLogDataFromCache", "onConnected", "run", "action", "pageView", "trace") }) m.addInstructions(0, "return-void")
        stop(LOG, "setEnableConnect", "(Z)V", "const/4 p0, 0")
        stop(LOG, "setEnableConnectImpl", "(Z)V", "const/4 p1, 0")
        if (local == true) {
            for (m in mutableClassDefBy("Lcom/intsig/log/LogUtils;").methods.filter { it.returnType == "V" && !it.name.startsWith("<") }) m.addInstructions(0, "return-void")
            stop("Lcom/intsig/log4a/Logger;", "〇〇888", "(Lcom/intsig/log4a/Level;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V")
        }
    }
}

private val adsCleanup = booleanOption(key = "manifestCleanup", default = true, title = "Remove ad-only manifest components", required = true)
private val adsManifestPatch = resourcePatch {
    dependsOn(camscannerInputPatch)
    execute { if (adsCleanup.value == true) document("AndroidManifest.xml").use { removeAdComponents(it) } }
}
val camscannerAdsPatch = bytecodePatch(
    name = "CamScanner disable ads", description = "Experimental suppression of qualified ad SDK load/show entry points and optional ad-only manifest cleanup. Ad callbacks and app fallback require device testing.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch, adsManifestPatch)
    val cleanup by adsCleanup()
    execute {
        for (t in adTargets) {
            val ret = t.descriptor.substringAfter(')')
            val code = when (ret) { "V" -> "return-void"; "Z", "I" -> "const/4 v0, 0\nreturn v0"; else -> error("Unqualified ad return type: $ret") }
            stop(t.type, t.name, t.descriptor, code)
        }
        stop("Lcom/intsig/camscanner/ads/csAd/CsAdUtil;", "O8", "(Ljava/lang/String;Lcom/intsig/camscanner/ads/csAd/bean/CsAdCommonBean;)Z", "const/4 v0, 0\nreturn v0")
    }
}
val camscannerOfflinePatch = resourcePatch(
    name = "CamScanner offline mode", description = "Revoke all app INTERNET declarations. Includes ad suppression; preserves real platform connectivity. External apps/services and offline error flows require device testing.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch, camscannerAdsPatch)
    execute { document("AndroidManifest.xml").use { removeInternet(it) } }
}

private val scannerNativePatch = resourcePatch {
    dependsOn(camscannerInputPatch)
    execute {
        val libs = listApkEntries("lib/").filter { it.endsWith("/libscannercs.so") }
        require(libs.isNotEmpty() && libs.all { it in scannerEdits }) { "Unqualified scanner ABI" }
        // Validate every ABI before writing any library.
        val replacements = libs.associateWith { editScanner(get(it).readBytes(), scannerEdits.getValue(it)) }
        replacements.forEach { (path, bytes) -> get(path).writeBytes(bytes) }
    }
}
val camscannerStartupPatch = bytecodePatch(
    name = "CamScanner scanner initialization compatibility", description = "Experimental re-signed scanner initialization compatibility for exact qualified native binaries, plus the local pirate-app prompt gate. Not an all-device crop fix.", default = false
) {
    compatibleWith(target)
    availability { installer, _ -> noNetwork(installer) }
    dependsOn(camscannerInputPatch, scannerNativePatch)
    execute { stop("Lcom/intsig/camscanner/business/PirateAppControl;", "O8", "(Landroid/content/Context;Lcom/intsig/callback/DialogDismissListener;)Z", "const/4 v0, 0\nreturn v0") }
}
