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
    val literal = quotaProbeScript.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
    return """
        if-eqz p1, :quota_probe_original
        if-eqz p2, :quota_probe_original
        const-string v0, "https://mo.camscanner.com/"
        invoke-virtual {p2, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
        move-result v0
        if-nez v0, :quota_probe_host
        const-string v0, "https://www.camscanner.com/"
        invoke-virtual {p2, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
        move-result v0
        if-eqz v0, :quota_probe_original
        :quota_probe_host
        const-string v0, "$literal"
        const/4 v1, 0x0
        invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
        :quota_probe_original
        nop
    """.trimIndent()
}

/** Explicit temporary diagnosis only. Never draws, grants or rewrites a server response. */
internal val quotaProbeScript = """
(function(importer) {
  "use strict";
  if (location.hostname !== "mo.camscanner.com" && location.hostname !== "www.camscanner.com") return;
  if (window.__csQuotaProbeV16) return;
  window.__csQuotaProbeV16 = true;
  var done = false, started = false, observer, watchTimer, timer, restore = function() {};
  var report = {ROUTE:"PENDING", QUERY:"NOT_RUN", HTTP_OBS:"NO_CAPTURE", RET_OBS:"UNKNOWN", XERR_OBS:"UNKNOWN", RAW_OBS:"UNKNOWN", OBSERVED_READS:0, QUOTA_READS:0};
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
  function render() {
    try {
      var box = document.getElementById("cs-quota-probe-v16");
      if (!box) {
        box = document.createElement("pre"); box.id = "cs-quota-probe-v16";
        box.style.cssText = "position:fixed;top:52px;left:8px;right:8px;z-index:2147483647;background:#102b32;color:#fff;padding:12px;font:13px/1.5 monospace;white-space:pre-wrap;pointer-events:none;border:2px solid #39c7b3";
        document.documentElement.appendChild(box);
      }
      box.textContent = "CS-QUOTA-V16 (one read; no draw)\n" + Object.keys(report).map(function(k) { return k + "=" + report[k]; }).join("\n");
    } catch (_) {}
  }
  function finish(state) {
    if (done) return;
    if (state) report.QUERY = state;
    done = true; clearTimeout(timer); clearTimeout(watchTimer);
    if (observer) observer.disconnect(); restore(); render();
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
    if (typeof XMLHttpRequest === "undefined") return;
    var original = XMLHttpRequest.prototype.open;
    function open(method, url) {
      if (!done && scope(url, method, legacy)) {
        this.addEventListener("loadend", function() {
          if (done) return;
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
    restore = function() { if (XMLHttpRequest.prototype.open === open) XMLHttpRequest.prototype.open = original; };
  }
  async function run(legacy) {
    report.ROUTE = legacy ? "LEGACY_WATCHAD_VIP" : "MODERN_DAY_COUNT";
    var q = new URLSearchParams(location.search);
    report.KEY_PRESENT = q.has("intsig_key") && !!q.get("intsig_key") ? "YES" : "NO";
    report.UID_PRESENT = q.has("encrypt_uid") && !!q.get("encrypt_uid") ? "YES" : "NO";
    render();
    timer = setTimeout(function() { finish("TIMEOUT_NO_RETRY"); }, 10000);
    var url = legacy ?
      "https://static-cdn.camscanner.com/camscanner-mo/static/pc-api-BrpmLW-U.js" :
      "https://static-cdn.camscanner.com/camscanner-integral-mo/static/config-Dyz9RPJ4.js";
    var hash = legacy ? "be6d3ff01592d1e952392801b2f5536cda89486f451cc90d1d060c219c23b636" :
      "29cb6e0058ef876d76b6dd60f611d54cc8cb62ab6a6a637578c1d34b5978c646";
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
        loaded = entries.some(function(e) { return e.name === url; });
        if (loaded) break;
        await new Promise(function(resolve) { setTimeout(resolve,100); });
      }
      if (done) return;
      if (!loaded) return finish("MODULE_NOT_LOADED");
      if (!window.crypto || !window.crypto.subtle) return finish("NO_HASH_API");
      var response = await fetch(url, {credentials:"omit"});
      if (done) return;
      if (!response.ok) return finish("MODULE_FETCH_FAILED");
      var bytes = await response.arrayBuffer();
      if (bytes.byteLength > 100000) return finish("MODULE_SIZE_REJECTED");
      var digest = await crypto.subtle.digest("SHA-256", bytes);
      var actual = Array.from(new Uint8Array(digest)).map(function(n) { return n.toString(16).padStart(2,"0"); }).join("");
      if (done) return;
      if (actual !== hash) return finish("MODULE_HASH_REJECTED");
      var mod = await importer(url);
      if (done) return;
      var fn = legacy ? mod.queryLottery : mod.I;
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
  observer = new MutationObserver(check);
  observer.observe(document.documentElement, {childList:true,subtree:true});
  watchTimer = setTimeout(function() { if (observer) observer.disconnect(); }, 30000);
  check();
})(function(url) { return import(url); });
""".trimIndent()
