package app.pigfoot.patches.camscanner

import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.AccessFlags

internal const val QUOTA_CLIENT = "Lcom/intsig/webview/WebViewFragment\$IsWebViewClient;"
internal const val QUOTA_CLIENT_PIN = "424a01b5ab60296ce8794c67b559992f6c76545b0a19fe0e19d2061e94341fcb"

internal fun requireQuotaProbeMethod(method: Method) {
    require(method.definingClass == QUOTA_CLIENT && method.name == "onPageFinished" &&
        method.parameterTypes.map { it.toString() } == listOf("Landroid/webkit/WebView;", "Ljava/lang/String;") &&
        method.returnType == "V" && !AccessFlags.STATIC.isSet(method.accessFlags) && method.implementation?.registerCount == 11) {
        "Unsupported quota probe page-finished method"
    }
}

internal fun quotaProbePrefix(): String {
    fun literal(script: String) = script.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    return """
        if-eqz p1, :quota_probe_original
        const/4 v3, 0x0
        const-string v1, "CS-Q23:HOOK NO_PAGE_URL"
        if-eqz p2, :quota_probe_toast
        const-string v1, "CS-Q23:HOOK HOST_OUT_OF_SCOPE"
        const-string v0, "https://mo.camscanner.com/"
        invoke-virtual {p2, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
        move-result v0
        if-nez v0, :quota_probe_host
        const-string v0, "https://www.camscanner.com/"
        invoke-virtual {p2, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
        move-result v0
        if-eqz v0, :quota_probe_toast
        :quota_probe_host
        const/4 v3, 0x1
        const-string v1, "CS-Q23:HOOK SCOPED"
        :quota_probe_toast
        invoke-virtual {p1}, Landroid/view/View;->getContext()Landroid/content/Context;
        move-result-object v0
        const/4 v2, 0x1
        invoke-static {v0, v1, v2}, Landroid/widget/Toast;->makeText(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;
        move-result-object v0
        invoke-virtual {v0}, Landroid/widget/Toast;->show()V
        if-eqz v3, :quota_probe_original
        ${nativeRewardClassCheckPrefix()}
        const-string v0, "${literal(quotaBootstrapScript)}"
        const/4 v1, 0x0
        invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
        const-string v0, "${literal(quotaProbeScript)}"
        invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
        :quota_probe_original
        nop
    """.trimIndent()
}

/** Independent, query-free liveness marker; never overwrites an existing report. */
internal val quotaBootstrapScript = """
(function() {
  if (location.hostname !== "mo.camscanner.com" && location.hostname !== "www.camscanner.com") return;
  if (!document.documentElement || document.getElementById("cs-quota-probe-v23")) return;
  var box = document.createElement("pre"); box.id = "cs-quota-probe-v23";
  box.style.cssText = "position:fixed;top:52px;left:8px;right:8px;z-index:2147483647;background:#102b32;color:#fff;padding:12px;font:13px/1.5 monospace;white-space:pre-wrap;overflow-wrap:anywhere;pointer-events:none;border:2px solid #39c7b3";
  box.textContent = "CS-QUOTA-V23\nSTAGE=JS_BOOTSTRAP";
  document.documentElement.appendChild(box);
})();
""".trimIndent()

/** Explicit temporary diagnosis only. Never draws, grants or rewrites a server response. */
internal val quotaProbeScript = """
(function(importer) {
  "use strict";
  if (location.hostname !== "mo.camscanner.com" && location.hostname !== "www.camscanner.com") return;
  if (window.__csQuotaProbeV23) return;
  window.__csQuotaProbeV23 = true;
  var done = false, started = false, observer, watchTimer, timer, uiQuery, observing = false, uiWatchStarted = false, uiWatchActive = false, uiTickTimer, uiDeadline, uiSamples = 0, restore = function() {};
  var nativeCheck = window.__csRewardClassCheck;
  if (typeof nativeCheck !== "string" || !/^(BOTH_RESOLVED|(CONTROL|CALLBACK)_(VERIFY_ERROR|CLASS_MISSING|LINKAGE_ERROR|SECURITY_ERROR|OTHER_ERROR))$/.test(nativeCheck)) nativeCheck = "UNKNOWN";
  var report = {NATIVE_CLASS_CHECK:nativeCheck, STAGE:"MAIN_ENTERED", ROUTE:"PENDING", QUERY:"NOT_RUN", HTTP_OBS:"NO_CAPTURE", RET_OBS:"UNKNOWN", XERR_OBS:"UNKNOWN", RAW_OBS:"UNKNOWN", OBSERVED_READS:0, QUOTA_READS:0};
  function code(v) {
    if (typeof v !== "number" && typeof v !== "string") return "UNKNOWN";
    var n = Number(v);
    return v !== "" && Number.isInteger(n) && n >= 0 && n <= 999999 ? String(n) : "OTHER";
  }
  function count(v, present) {
    if (!present) return "MISSING";
    if (v === null) return "NULL";
    if (typeof v === "number" && Number.isInteger(v) && v >= 0 && v <= 100) return "COUNT_" + v;
    if (typeof v === "string" && v.trim() !== "" && Number.isInteger(Number(v)) && Number(v) >= 0 && Number(v) <= 100) return "COUNT_" + Number(v);
    return "INVALID_TYPE";
  }
  // Passive Vue inspection: no methods, setters, credentials or objects are output.
  async function installInitGuard() {
    await ($lotteryInitGuardFactory)(importer, report, render, function() {
      Promise.resolve().then(function() { if (!uiWatchStarted) captureUi("UI_AFTER"); render(); });
      startUiWatch();
    });
  }
  function uiSnapshot(q) {
    try {
      var root = document.getElementById("app");
      if (!root || !root._vnode) return {state:"NO_ROOT"};
      var queue = [root._vnode], seen = new Set(), found = [];
      while (queue.length && seen.size < 200) {
        var node = queue.shift();
        if (!node || typeof node !== "object" || seen.has(node)) continue;
        seen.add(node);
        var instance = node.component;
        if (instance) {
          var data = instance.data, methods = instance.type && instance.type.methods;
          if (data && methods && typeof methods.queryLotteryTime === "function" &&
              typeof methods.startlottery === "function" && typeof methods.banLottery === "function" &&
              Object.prototype.hasOwnProperty.call(data,"lotteryChance") &&
              Object.prototype.hasOwnProperty.call(data,"canReady") &&
              Object.prototype.hasOwnProperty.call(data,"intsig_key") &&
              Object.prototype.hasOwnProperty.call(data,"encrypt_uid") && !found.includes(data)) found.push(data);
          if (instance.subTree) queue.push(instance.subTree);
        }
        [node.children,node.dynamicChildren].forEach(function(list) {
          if (Array.isArray(list)) {
            for (var i=0; i<list.length && queue.length<=400; i++) queue.push(list[i]);
          }
        });
        if (queue.length > 400) return {state:"LIMIT"};
      }
      if (queue.length) return {state:"LIMIT"};
      if (found.length !== 1) return {state:found.length ? "AMBIGUOUS" : "NO_COMPONENT"};
      var d = found[0];
      function kind(v) { return Array.isArray(v) ? "ARRAY" : typeof v === "string" ? "STRING" : "OTHER"; }
      function flag(v) { return v === true ? "YES" : v === false ? "NO" : "UNKNOWN"; }
      return {state:count(d.lotteryChance,true)+"/READY_"+flag(d.canReady)+
          "/TZ_"+(typeof d.timezone === "string" && d.timezone ? "SET" : "EMPTY")+
          "/EXHAUSTED_"+flag(d.showChance),
        keyKind:kind(d.intsig_key), uidKind:kind(d.encrypt_uid),
        keyMatch:typeof d.intsig_key === "string" ? (d.intsig_key === q.get("intsig_key") ? "YES" : "NO") : "NOT_STRING",
        uidMatch:typeof d.encrypt_uid === "string" ? (d.encrypt_uid === q.get("encrypt_uid") ? "YES" : "NO") : "NOT_STRING"};
    } catch (_) { return {state:"UNAVAILABLE"}; }
  }
  function captureUi(label) {
    if (!uiQuery) return;
    var ui = uiSnapshot(uiQuery); report[label] = ui.state;
    if (ui.keyKind) {
      report.UI_KEY_TYPE = ui.keyKind; report.UI_UID_TYPE = ui.uidKind;
      report.UI_KEY_MATCH = ui.keyMatch; report.UI_UID_MATCH = ui.uidMatch;
    }
  }
  function releaseObservation() {
    observing = false;
    try { restore(); } catch (_) {}
  }
  function stopUiWatch(reason) {
    if (!uiWatchActive) return;
    if (reason === "FINAL" && location.pathname !== "/lottery/relottery") reason = "ROUTE_CHANGED";
    if (reason === "FINAL") sampleUi();
    uiWatchActive = false; clearTimeout(uiTickTimer); clearTimeout(uiDeadline);
    if (typeof window.removeEventListener === "function") window.removeEventListener("pagehide",hideUiWatch);
    releaseObservation(); report.UI_OBSERVATION = reason; render();
  }
  function hideUiWatch() { stopUiWatch("PAGE_HIDDEN"); }
  function sampleUi() {
    captureUi("UI_AFTER"); report.UI_SAMPLES = ++uiSamples;
  }
  function startUiWatch() {
    if (uiWatchStarted || !uiQuery || report.ROUTE !== "LEGACY_WATCHAD_VIP") return;
    uiWatchStarted = true; uiWatchActive = true;
    report.UI_OBSERVATION = "LIVE"; report.UI_WINDOW_SECONDS = 35;
    function tick() {
      if (!uiWatchActive) return;
      if (location.pathname !== "/lottery/relottery") return stopUiWatch("ROUTE_CHANGED");
      sampleUi(); render(); uiTickTimer = setTimeout(tick,1000);
    }
    if (typeof window.addEventListener === "function") window.addEventListener("pagehide",hideUiWatch,{once:true});
    uiDeadline = setTimeout(function() { stopUiWatch("FINAL"); },35000); tick();
  }
  function render() {
    try {
      var box = document.getElementById("cs-quota-probe-v23");
      if (!box) {
        box = document.createElement("pre"); box.id = "cs-quota-probe-v23";
        box.style.cssText = "position:fixed;top:52px;left:8px;right:8px;z-index:2147483647;background:#102b32;color:#fff;padding:12px;font:13px/1.5 monospace;white-space:pre-wrap;overflow-wrap:anywhere;pointer-events:none;border:2px solid #39c7b3";
        document.documentElement.appendChild(box);
      }
      box.textContent = "CS-QUOTA-V23 (no draw)\n" + Object.keys(report).map(function(k) { return k + "=" + report[k]; }).join("\n");
    } catch (_) {}
  }
  function finish(state) {
    if (done) return;
    if (state) report.QUERY = state;
    done = true; clearTimeout(timer); clearTimeout(watchTimer);
    if (observer) observer.disconnect();
    if (!uiWatchActive) releaseObservation();
    if (!uiWatchStarted) captureUi("UI_AFTER"); render();
  }
  function scope(url, method, legacy) {
    try {
      var u = new URL(String(url), location.href);
      return String(method || "GET").toUpperCase() === "GET" && (legacy ?
        u.pathname.endsWith("/query_property") && u.searchParams.get("property_id") === "watchad_vip" :
        u.pathname.endsWith("/reward/big_lottery"));
    } catch (_) { return false; }
  }
  function observe(legacy) {
    if (typeof XMLHttpRequest === "undefined" || observing) return;
    if (legacy && uiWatchStarted && !uiWatchActive) return;
    var descriptor = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype,"open");
    if (!descriptor || !descriptor.writable || typeof descriptor.value !== "function") return;
    var original = descriptor.value; observing = true;
    function open(method, url) {
      if (observing && (!legacy || location.pathname === "/lottery/relottery") && scope(url, method, legacy)) {
        this.addEventListener("loadend", function() {
          if (!observing) return;
          report.OBSERVED_READS++; report.HTTP_OBS = code(this.status);
          try { report.XERR_OBS = code(this.getResponseHeader("X-IS-Error-Code")); } catch (_) {}
          try {
            var body = this.responseType === "json" ? this.response : JSON.parse(this.responseText);
            report.RET_OBS = code(body && body.ret);
            var data = body && body.data;
            var key = legacy ? "watchad_vip_chance" : "day_count";
            report.RAW_OBS = count(data && data[key], !!data && Object.prototype.hasOwnProperty.call(data, key));
          } catch (_) { report.RAW_OBS = "BODY_UNAVAILABLE"; }
          render();
        }, {once:true});
      }
      return Reflect.apply(original, this, arguments);
    }
    XMLHttpRequest.prototype.open = open;
    restore = function() {
      var current = Object.getOwnPropertyDescriptor(XMLHttpRequest.prototype,"open");
      if (current && current.value === open && current.writable) XMLHttpRequest.prototype.open = original;
    };
  }
  async function run(legacy) {
    report.ROUTE = legacy ? "LEGACY_WATCHAD_VIP" : "MODERN_DAY_COUNT";
    var q = new URLSearchParams(location.search); uiQuery = q;
    captureUi("UI_BEFORE");
    if (legacy) { startUiWatch(); observe(true); await installInitGuard(); }
    report.KEY_PARAMS = Math.min(q.getAll("intsig_key").length,9);
    report.UID_PARAMS = Math.min(q.getAll("encrypt_uid").length,9);
    report.BRIDGE_READY = window.jsapi_ready === true ? "YES" : window.jsapi_ready === false ? "NO" : "UNKNOWN";
    report.KEY_PRESENT = q.has("intsig_key") && !!q.get("intsig_key") ? "YES" : "NO";
    report.UID_PRESENT = q.has("encrypt_uid") && !!q.get("encrypt_uid") ? "YES" : "NO";
    render();
    timer = setTimeout(function() { finish("TIMEOUT_NO_RETRY"); }, 10000);
    // Exact, independently reviewed public builds; never import a reported tag blindly.
    var profiles = legacy ? [
      {url:"https://static-cdn.camscanner.com/camscanner-mo/static/pc-api-BrpmLW-U.js",
       hash:"be6d3ff01592d1e952392801b2f5536cda89486f451cc90d1d060c219c23b636", entry:"queryLottery"},
      {url:"https://static-cdn.camscanner.com/camscanner-mo/static/pc-api-DWt5-aHp.js",
       hash:"cb0dd1b74bc22ab7710971fdd2d78ee1d108bf6189f9eda74d0e80049f910cce", entry:"i"}
    ] : [
      {url:"https://static-cdn.camscanner.com/camscanner-integral-mo/static/config-Dyz9RPJ4.js",
       hash:"29cb6e0058ef876d76b6dd60f611d54cc8cb62ab6a6a637578c1d34b5978c646", entry:"I"}
    ];
    var profile;
    var phase = "MODULE";
    try {
      var loaded = false;
      for (var attempt = 0; attempt < 10 && !done; attempt++) {
        var entries = performance.getEntriesByType("resource");
        var folder = legacy ? "/camscanner-mo/static/" : "/camscanner-integral-mo/static/";
        var base = legacy ? "pc-api-" : "config-";
        report.MODULE_TAG = "NONE";
        entries.forEach(function(entry) {
          try {
            var asset = new URL(entry.name);
            var file = asset.pathname.slice(folder.length);
            var tag = file.slice(base.length,-3);
            var alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789_-";
            if (asset.origin === "https://static-cdn.camscanner.com" && asset.pathname.startsWith(folder) &&
                file.startsWith(base) && file.endsWith(".js") && tag.length >= 8 && tag.length <= 12 &&
                Array.from(tag).every(function(c) { return alphabet.includes(c); })) report.MODULE_TAG = file;
          } catch (_) {}
        });
        var matches = profiles.filter(function(p) { return entries.some(function(e) { return e.name === p.url; }); });
        if (matches.length > 1) return finish("MODULE_AMBIGUOUS_NO_READ");
        profile = matches[0]; loaded = !!profile;
        if (loaded) break;
        await new Promise(function(resolve) { setTimeout(resolve,100); });
      }
      if (done) return;
      if (!loaded) return finish("MODULE_NOT_LOADED");
      var url = profile.url;
      report.MODULE_TAG = url.slice(url.lastIndexOf("/") + 1);
      if (legacy && profile.entry === "i") {
        report.MODULE_CHECK = "PASSIVE_NO_FUNCTION_IMPORT";
        return finish("PASSIVE_ORIGINAL_READS_ONLY");
      }
      if (!window.crypto || !window.crypto.subtle) return finish("NO_HASH_API");
      var response = await fetch(url, {credentials:"omit"});
      if (done) return;
      if (!response.ok) return finish("MODULE_FETCH_FAILED");
      var bytes = await response.arrayBuffer();
      if (bytes.byteLength > 100000) return finish("MODULE_SIZE_REJECTED");
      var digest = await crypto.subtle.digest("SHA-256", bytes);
      var actual = Array.from(new Uint8Array(digest)).map(function(n) { return n.toString(16).padStart(2,"0"); }).join("");
      if (done) return;
      if (actual !== profile.hash) return finish("MODULE_HASH_REJECTED");
      var mod = await importer(url);
      if (done) return;
      var fn = mod[profile.entry];
      if (typeof fn !== "function") return finish("FUNCTION_MISSING");
      observe(legacy);
      report.QUERY = "PENDING_ONE_READ"; render();
      var args;
      if (legacy) args = {intsig_key:q.get("intsig_key") || "",encrypt_uid:q.get("encrypt_uid") || "",client_app:q.get("client_app") || "",timezone:String(-new Date().getTimezoneOffset()/60)};
      else {
        var token = mod.c && mod.c.macro && mod.c.macro.token;
        report.TOKEN_PRESENT = token ? "YES" : "NO";
        if (!token) return finish("NO_TOKEN");
        args = {token:token,time_zone:q.get("time_zone") || "",cs_ept_d:q.get("cs_ept_d") || ""};
        if (q.get("version") === "new") args.lottery_for_new = 1;
      }
      phase = "QUOTA_READ"; report.QUOTA_READS = 1;
      var result = await fn(args);
      if (done) return;
      var data = result && result.data;
      var key = legacy ? "watchad_vip_chance" : "day_count";
      if (!legacy && result) report.QUERY_RET = code(result.ret);
      report.QUERY = data === false ? "QUERY_FALSE" : count(data && data[key], !!data && Object.prototype.hasOwnProperty.call(data,key));
      render();
      setTimeout(function() { finish(); }, 100);
    } catch (_) { finish(phase === "QUOTA_READ" ? "QUERY_ERROR_NO_DETAILS" : "MODULE_ERROR_NO_READ"); }
  }
  function check() {
    if (started) return;
    var path = location.pathname;
    var legacy = path === "/lottery/relottery" || path === "/lottery/relottery/";
    var modern = path === "/integral/intergralTurntable" || path === "/integral/intergralTurntable/";
    if (legacy || modern) {
      started = true; if (observer) observer.disconnect(); clearTimeout(watchTimer); run(legacy);
    } else if (path.toLowerCase().includes("lottery") || document.querySelector(".turn-lottery,.lottery")) {
      started = true; report.ROUTE = "OTHER_LOTTERY"; finish("UNQUALIFIED_PAGE_NO_READ");
    }
  }
  render();
  observer = new MutationObserver(check);
  observer.observe(document.documentElement, {childList:true,subtree:true});
  watchTimer = setTimeout(function() { if (!started) { started = true; report.ROUTE = "UNRECOGNIZED"; finish("ROUTE_NOT_RECOGNIZED_NO_READ"); } }, 30000);
  check();
})(function(url) { return import(url); });
""".trimIndent()
