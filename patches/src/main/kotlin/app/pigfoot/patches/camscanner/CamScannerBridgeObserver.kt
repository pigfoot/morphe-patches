package app.pigfoot.patches.camscanner

/** Diagnostic transport only. Ordinary SDK callbacks are forwarded without interpretation. */
internal val quotaBridgeObserverFunctions = """
  var nativeListenerAttempted = false;
  function qualifiedGuardRender() { try { render(); } catch (_) {} }
  function armNativeReceiver(mod) {
    if (nativeListenerAttempted) return;
    nativeListenerAttempted = true;
    var bridge, original, wrapped, deadline, active = false;
    function finish(reason) {
      active = false;
      try { clearTimeout(deadline); } catch (_) {}
      try {
        var current = Object.getOwnPropertyDescriptor(bridge,"callWeb");
        if (!current || current.value !== wrapped) report.NATIVE_CLEANUP = reason + "_OWNER_CHANGED";
        else if (!current.writable) report.NATIVE_CLEANUP = reason + "_RELEASED_NOT_RESTORED";
        else { Object.defineProperty(bridge,"callWeb",original); report.NATIVE_CLEANUP = reason + "_RESTORED"; }
      } catch (_) { report.NATIVE_CLEANUP = reason + "_RELEASED_NOT_RESTORED"; }
      qualifiedGuardRender();
    }
    try {
      var owner = Object.getOwnPropertyDescriptor(window,"JSMethods");
      if (!owner || !owner.value || typeof owner.value !== "object") { report.NATIVE_LISTENER = "NO_OWN_BRIDGE"; return; }
      bridge = owner.value;
      original = Object.getOwnPropertyDescriptor(bridge,"callWeb");
      if (!original || typeof original.value !== "function" || !original.writable ||
          !mod || !mod.C || mod.C.callWeb !== original.value || Function.prototype.toString.call(original.value) !== "function je(e){U(\"callback\");const n=new At().decode(e);let i;U(\"callback data \"+n);try{i=JSON.parse(n)}catch(p){se(\"error\",{error:n});try{i=JSON.parse(n.replace(/\\n/g,\"\\\\n\").replace(/\\r/g,\"\"))}catch(g){return se(\"error\",{error:\"json解析失败\"}),\"failed\"}}const r=parseInt(i.id),u=ge[r];if(u){const p=u.actionId;se(\"success--\".concat(p),{success:JSON.stringify(n)}),u.call(window,i),delete ge[r]}return\"success\"}") {
        report.NATIVE_LISTENER = "UNQUALIFIED_NO_CHANGE"; return;
      }
      wrapped = function() {
        var args = arguments, value, tag;
        if (active && this === bridge && location.pathname === "/lottery/relottery" && args.length === 1 && typeof args[0] === "string" && args[0].length <= 1024) {
          try {
            var current = Object.getOwnPropertyDescriptor(window,"JSMethods");
            value = JSON.parse(atob(args[0]));
            if (current && current.value === bridge && value && Object.keys(value).length === 1 && Object.prototype.hasOwnProperty.call(value,"__cs_native_preload_v25")) tag = value.__cs_native_preload_v25;
          } catch (_) {}
        }
        if (typeof tag === "string" && /^(ENTERED|THREW_(NULL_POINTER|ILLEGAL_ARGUMENT|ILLEGAL_STATE|SECURITY|VERIFY|CLASS_MISSING|METHOD_MISSING|LINK_ERROR|INITIALIZER|OTHER)|SITE_(BRIDGE_ENTRY|ROUTER_SERVICE|APP_DISPATCH|WATCH_CONTROL|LEFT_MANAGER|REWARD_BASE|POSITION_ADAPTER|JSON_PARSER|NULL_CHECK|OTHER))$/.test(tag)) {
          try {
            if (tag === "ENTERED") { report.NATIVE_ENTRY = "OBSERVED"; report.NATIVE_ENTRIES++; }
            else if (tag.startsWith("THREW_")) report.NATIVE_THROW = tag.slice(6);
            else { report.NATIVE_SITE = tag.slice(5); finish("OBSERVED"); }
            qualifiedGuardRender();
          } catch (_) {}
          return "success";
        }
        return Reflect.apply(original.value,this,args);
      };
      active = true;
      bridge.callWeb = wrapped;
      var installed = Object.getOwnPropertyDescriptor(bridge,"callWeb");
      if (!installed || installed.value !== wrapped) { active = false; report.NATIVE_LISTENER = "INSTALL_NOT_CONFIRMED"; return; }
      report.NATIVE_LISTENER = "INSTALLED_OWN_JS";
      report.NATIVE_CLEANUP = "PENDING";
      deadline = setTimeout(function() { finish("DEADLINE"); },35000);
    } catch (_) { report.NATIVE_LISTENER = "UNAVAILABLE"; if (active) finish("INSTALL_FAILED"); }
    qualifiedGuardRender();
  }
""".trimIndent()
