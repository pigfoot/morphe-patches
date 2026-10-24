package app.pigfoot.patches.camscanner

/** Passive observation of the original qualified preload; never calls a bridge on its own. */
internal val quotaBridgeObserverFunctions = """
  var callObserverAttempted = false;
  function bridgeObserverRender() { try { render(); } catch (_) {} }
  function armBridgeObserver() {
    if (callObserverAttempted || !/^INSTALLED_(EARLY|WITH_ROOT)$/.test(report.INIT_GUARD || "")) return;
    callObserverAttempted = true;
    var bridge, original, wrapped, deadline, active = false;
    function errorKind(e) {
      try { return ["TypeError","SecurityError","ReferenceError","RangeError","Error"].includes(e && e.name) ? e.name : "OTHER"; }
      catch (_) { return "OTHER"; }
    }
    function finish(reason) {
      active = false;
      try { clearTimeout(deadline); } catch (_) {}
      try {
        var current = Object.getOwnPropertyDescriptor(bridge,"callApp");
        if (!current || current.value !== wrapped) report.CALLAPP_CLEANUP = reason + "_OWNER_CHANGED";
        else if (!current.writable) report.CALLAPP_CLEANUP = reason + "_RELEASED_NOT_RESTORED";
        else { Object.defineProperty(bridge,"callApp",original); report.CALLAPP_CLEANUP = reason + "_RESTORED"; }
      } catch (_) { report.CALLAPP_CLEANUP = reason + "_RELEASED_NOT_RESTORED"; }
      bridgeObserverRender();
    }
    try {
      var global = Object.getOwnPropertyDescriptor(window,"JSMethods");
      if (!global || !global.value || typeof global.value !== "object") {
        report.CALLAPP_OBSERVER = "NO_OWN_BRIDGE_NO_CHANGE"; return;
      }
      bridge = global.value;
      original = Object.getOwnPropertyDescriptor(bridge,"callApp");
      if (!original || typeof original.value !== "function" || !original.writable) {
        report.CALLAPP_OBSERVER = "UNSAFE_METHOD_NO_CHANGE"; return;
      }
      var parse = JSON.parse;
      wrapped = function() {
        var args = arguments, match = false;
        if (active && this === bridge && location.pathname === "/lottery/relottery" && args.length === 1 && typeof args[0] === "string" && args[0].length <= 8192) {
          try {
            var owner = Object.getOwnPropertyDescriptor(window,"JSMethods");
            var value = parse(args[0]);
            match = owner && owner.value === bridge && value && value.action === "watchad_lottery" && value.data && value.data.prestrain === 1 && value.close_web === "0";
          } catch (_) {}
        }
        if (!match) return Reflect.apply(original.value,this,args);
        report.CALLAPP_PRELOAD_CALLS++;
        report.CALLAPP_BOUNDARY = "ENTERED_ORIGINAL";
        bridgeObserverRender();
        try {
          var result = Reflect.apply(original.value,this,args);
          report.CALLAPP_BOUNDARY = "RETURNED_ORIGINAL";
          return result;
        } catch (e) {
          report.CALLAPP_BOUNDARY = "THREW_" + errorKind(e);
          throw e;
        } finally { finish("OBSERVED"); }
      };
      active = true;
      bridge.callApp = wrapped;
      var installed = Object.getOwnPropertyDescriptor(bridge,"callApp");
      if (!installed || installed.value !== wrapped) {
        active = false; report.CALLAPP_OBSERVER = "INSTALL_NOT_CONFIRMED"; return;
      }
      report.CALLAPP_OBSERVER = "INSTALLED_OWN_WRITABLE";
      report.CALLAPP_CLEANUP = "PENDING";
      deadline = setTimeout(function() { finish("DEADLINE"); },30000);
    } catch (_) {
      report.CALLAPP_OBSERVER = "UNAVAILABLE";
      if (active) finish("INSTALL_FAILED");
    }
  }
  function qualifiedGuardRender() {
    try { armBridgeObserver(); } catch (_) {}
    bridgeObserverRender();
  }
""".trimIndent()
