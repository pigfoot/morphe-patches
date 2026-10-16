package app.pigfoot.diagnostics;

/** Bounded process-local observations, NOT document-correlated network tracing. */
public final class WordTrace {
    private static final String[] events = new String[8];
    private static final long[] times = new long[8];
    private static int count;
    private static String loginDetails;
    private static int loginCode;
    private static long loginTime;
    private static final java.lang.ref.WeakReference<?>[] linkedKeys = new java.lang.ref.WeakReference<?>[16];
    private static final String[] linkedDetails = new String[16];
    private static final int[] linkedCodes = new int[16];
    private static final long[] linkedTimes = new long[16];
    private static int linkedNext;
    private static final ThreadLocal<String[]> pendingLogin = new ThreadLocal<>();
    private WordTrace() {}

    /** Treat only the observed obsolete UAPI cache as an original cache miss.
     * No identity, payload, preference or diagnostic record is retained. */
    public static String refreshLoginApis(String cached) {
        if (cached == null || cached.isEmpty() || cached.length() > 32768) return cached;
        try {
            Class<?> json = Class.forName("org.json.JSONObject");
            Object object = json.getConstructor(String.class).newInstance(cached);
            Object uapi = json.getMethod("opt", String.class).invoke(object, "UAPI");
            return "https://api-cs-bak.intsig.net/user/cs".equals(uapi) ? null : cached;
        } catch (Throwable ignored) { return cached; }
    }

    // Only sanitized strings survive these calls; keys never strongly retain
    // application exceptions/events. Identity, not equals(), owns correlation.
    static synchronized void linkAt(Object key, int code, String details, long now) {
        if (key == null || details == null) return;
        int index = Math.floorMod(linkedNext++, linkedKeys.length);
        linkedKeys[index] = new java.lang.ref.WeakReference<>(key);
        linkedDetails[index] = details;
        linkedCodes[index] = code;
        linkedTimes[index] = now;
    }

    static synchronized String takeLinkedAt(Object key, int code, long now) {
        if (key == null) return null;
        for (int i = 0; i < linkedKeys.length; i++) {
            Object stored = linkedKeys[i] == null ? null : linkedKeys[i].get();
            long age = now - linkedTimes[i];
            boolean expired = stored == null || age < 0 || age > 30000000000L;
            if (expired || stored == key) {
                String result = !expired && linkedCodes[i] == code ? linkedDetails[i] : null;
                linkedKeys[i] = null;
                linkedDetails[i] = null;
                if (stored == key) return result;
            }
        }
        return null;
    }

    /** Called only immediately before the qualified HTTP-500 exception throw. */
    public static void loginHttp500(Throwable error, Object connection, int code) {
        try {
            if (error == null || code != 500) return;
            String details = "HTTP_STATUS=500\nERROR_HEADER=UNAVAILABLE\nERROR_MESSAGE_PRESENT=UNKNOWN";
            try {
                java.lang.reflect.Method getter = connection.getClass().getMethod("getHeaderField", String.class);
                String header = (String) getter.invoke(connection, "X-IS-Error-Code");
                String message = (String) getter.invoke(connection, "X-IS-Error-Msg");
                String state = header == null ? "MISSING" : header.isEmpty() ? "EMPTY" :
                        header.matches("-?[0-9]{1,8}") ? header : "NON_NUMERIC";
                details = "HTTP_STATUS=500\nERROR_HEADER=" + state +
                        "\nERROR_MESSAGE_PRESENT=" + (message != null && !message.isEmpty());
            } catch (Throwable ignored) { }
            details += "\n" + loginQueryRoute(error.getMessage());
            linkAt(error, code, details, System.nanoTime());
        } catch (Throwable ignored) { }
    }

    /** Passive observation at the original HTTP-406 generic business throw. */
    public static void loginHttp406(Throwable error, Object connection, int code, String requestUrl) {
        try {
            if (error == null || code != 201) return;
            String details = "ERROR_ORIGIN=HTTP_406_BUSINESS\nHTTP_STATUS=406\nERROR_HEADER=UNAVAILABLE\nERROR_MESSAGE_PRESENT=UNKNOWN";
            try {
                java.lang.reflect.Method getter = connection.getClass().getMethod("getHeaderField", String.class);
                String header = (String) getter.invoke(connection, "X-IS-Error-Code");
                String message = (String) getter.invoke(connection, "X-IS-Error-Msg");
                String state = header == null ? "MISSING" : header.isEmpty() ? "EMPTY" :
                        header.matches("-?[0-9]{1,8}") ? header : "NON_NUMERIC";
                details = "ERROR_ORIGIN=HTTP_406_BUSINESS\nHTTP_STATUS=406\nERROR_HEADER=" + state +
                        "\nERROR_MESSAGE_PRESENT=" + (message != null && !message.isEmpty());
            } catch (Throwable ignored) { }
            details += "\n" + loginQueryRoute(requestUrl == null ? null : requestUrl + " errorHeadCode:");
            linkAt(error, code, details, System.nanoTime());
        } catch (Throwable ignored) { }
    }

    /** Parse only the qualified formatter prefix; never retain a raw URL or query. */
    static String loginQueryRoute(String message) {
        String unavailable = "QUERY_ROUTE=UNAVAILABLE";
        try {
            if (message == null || message.length() > 32768) return unavailable + "\nQUERY_ROUTE_REASON=INPUT_LIMIT";
            // The qualified app overrides getMessage(): its URL formatter is
            // wrapped even though the HTTP branch constructor receives bare text.
            if (message.startsWith("TianshuException (")) {
                String prefix = "TianshuException (500, ";
                if (!message.startsWith(prefix) || !message.endsWith(")"))
                    return unavailable + "\nQUERY_ROUTE_REASON=WRAPPER";
                message = message.substring(prefix.length(), message.length() - 1);
            }
            int end = message.indexOf(" errorHeadCode:");
            if (end <= 0 || end > 16384) return unavailable + "\nQUERY_ROUTE_REASON=FORMAT";
            java.net.URI uri = new java.net.URI(message.substring(0, end));
            if (!"https".equals(uri.getScheme())) return unavailable + "\nQUERY_ROUTE_REASON=SCHEME";
            if (uri.getRawUserInfo() != null) return unavailable + "\nQUERY_ROUTE_REASON=USERINFO";
            if (uri.getFragment() != null) return unavailable + "\nQUERY_ROUTE_REASON=FRAGMENT";
            if (uri.getPort() != -1 && uri.getPort() != 443) return unavailable + "\nQUERY_ROUTE_REASON=PORT";
            String host = uri.getHost(), path = uri.getRawPath();
            if (host == null) return unavailable + "\nQUERY_ROUTE_REASON=HOST";
            // An unknown path must not suppress separately safe host/client metadata.
            path = "/user/cs/query".equals(path) ? "/user/cs/query" :
                    "/sync/query".equals(path) ? "/sync/query" : "/query".equals(path) ? "/query" : "OTHER";
            String hostClass = "api-cs-bak.intsig.net".equalsIgnoreCase(host) ? "ACCOUNT_BACKUP_PRODUCTION" :
                    "api-cs-bak-sandbox.intsig.net".equalsIgnoreCase(host) ? "ACCOUNT_BACKUP_SANDBOX" :
                    "api-cs.intsig.net".equalsIgnoreCase(host) ? "ACCOUNT_PRIMARY_PRODUCTION" :
                    "api-cs-us.intsig.net".equalsIgnoreCase(host) ? "ACCOUNT_US_PRODUCTION" :
                    "api-cs-sandbox.intsig.net".equalsIgnoreCase(host) ? "ACCOUNT_PRIMARY_SANDBOX" :
                    "cs-bak.intsig.net".equalsIgnoreCase(host) ? "DEFAULT_PRODUCTION" :
                    "cs-bak-sandbox.intsig.net".equalsIgnoreCase(host) ? "DEFAULT_SANDBOX" : "OTHER";
            String client = "MISSING";
            String email = "MISSING";
            int emailCount = 0;
            int found = 0;
            String query = uri.getRawQuery();
            if (query != null) for (String part : query.split("&", -1)) {
                int equal = part.indexOf('=');
                String key = java.net.URLDecoder.decode(equal < 0 ? part : part.substring(0, equal), "UTF-8");
                if ("emailEncrypt".equals(key)) {
                    if (++emailCount > 1) return unavailable + "\nQUERY_ROUTE_REASON=DUPLICATE_EMAIL";
                    String value = java.net.URLDecoder.decode(equal < 0 ? "" : part.substring(equal + 1), "UTF-8");
                    email = value.isEmpty() ? "EMPTY" : "PRESENT";
                }
                if (!"client_app".equals(key)) continue;
                if (++found > 1) return unavailable + "\nQUERY_ROUTE_REASON=DUPLICATE_CLIENT";
                String value = java.net.URLDecoder.decode(equal < 0 ? "" : part.substring(equal + 1), "UTF-8");
                // Exact qualified app/version plus the original builder's two flags only.
                client = value.matches("CamScanner_AD_1(FULL|LITE)@7\\.26\\.5\\.2609160000(\\.Debug)?") ? value : "OTHER";
            }
            return "QUERY_ROUTE=QUALIFIED_FORMATTER\nQUERY_HOST_CLASS=" + hostClass +
                    "\nQUERY_PATH=" + path + "\nCLIENT_APP=" + client + "\nQUERY_EMAIL_ENCRYPT=" + email;
        } catch (Throwable ignored) { return unavailable + "\nQUERY_ROUTE_REASON=PARSE_ERROR"; }
    }

    static String queryCaller(Throwable error) {
        try {
            int scanned = 0;
            for (StackTraceElement frame : error.getStackTrace()) {
                if (++scanned > 64) break;
                if (frame == null) continue;
                String owner = frame.getClassName();
                if (owner.equals("com.intsig.tsapp.account.viewmodel.EmailLoginViewModel$queryEmailAccountExist$1$1"))
                    return "ACCOUNT_EXISTENCE";
                if (owner.equals("com.intsig.tsapp.account.util.PwdLoginCommonControl$1") &&
                        frame.getMethodName().equals("operationLogin")) return "PASSWORD_PRECHECK";
            }
        } catch (Throwable ignored) { }
        return "UNKNOWN";
    }

    /** Synchronous handoff inside one original exception handler, not a global latest error. */
    public static void loginLinkedError(Throwable error, int code) {
        try {
            pendingLogin.remove();
            long now = System.nanoTime();
            String transport = takeLinkedAt(error, code, now);
            String details = "QUERY_CALLER=" + queryCaller(error) + "\n" + sanitizedLoginDetails(error) +
                    "\n" + (transport == null ? "TRANSPORT_NOT_LINKED" : transport);
            pendingLogin.set(new String[]{Integer.toString(code), Long.toString(now), details});
        } catch (Throwable ignored) { pendingLogin.remove(); }
    }

    /** Bind the exact ErrorMsg just constructed, before its original postValue. */
    public static void loginBind(Object event) {
        try {
            String[] pending = pendingLogin.get();
            pendingLogin.remove();
            if (pending == null) return;
            long now = System.nanoTime(), age = now - Long.parseLong(pending[1]);
            if (age < 0 || age > 30000000000L) return;
            linkAt(event, Integer.parseInt(pending[0]), pending[2], now);
        } catch (Throwable ignored) { pendingLogin.remove(); }
    }

    /** The UI event must be the same object; no code/time-only fallback. */
    public static String loginLinkedMessage(String message, int code, Object event) {
        try {
            String details = takeLinkedAt(event, code, System.nanoTime());
            if (message == null || message.contains("CS-LOGIN-DIAG")) return message;
            return message + "\nCS-LOGIN-DIAG-V8 code=" + code +
                    (details == null ? "\nUNLINKED_ERROR" : "\nERROR_OBJECT_LINKED\n" + details);
        } catch (Throwable ignored) { return message; }
    }

    /** Retain only sanitized, short-lived metadata, never the exception or message. */
    public static void loginError(Throwable error, int code) {
        loginErrorAt(error, code, System.nanoTime());
    }

    static synchronized void loginErrorAt(Throwable error, int code, long now) {
        loginDetails = sanitizedLoginDetails(error);
        loginCode = code;
        loginTime = now;
    }

    static String sanitizedLoginDetails(Throwable error) {
        try {
            if (error == null) return "DETAILS_UNAVAILABLE";
            String raw = error.getMessage();
            if (raw == null || raw.length() > 32768) return "DETAILS_UNAVAILABLE";
            String phase = raw.contains("/query?") ? "QUERY" : raw.contains("/login_c?") ? "LOGIN_C" : "OTHER";
            StringBuilder out = new StringBuilder("PATH_HINT=").append(phase);
            int head = raw.indexOf("errorHeadCode:");
            if (head >= 0) {
                java.util.regex.Matcher number = java.util.regex.Pattern.compile(
                        "errorHeadCode:\\s*(-?[0-9]{1,8})(?=\\s|$)").matcher(raw);
                out.append("\nHTTP500_MESSAGE_MARKER=true\nHEADER_CODE=")
                        .append(number.find() ? number.group(1) : "NON_NUMERIC");
                int start = raw.indexOf(" errorMsg:", head), end = raw.indexOf(" headerServer:", head);
                String flags = "UNAVAILABLE";
                if (start >= 0 && end >= start && end - start <= 2048) {
                    String text = raw.substring(start + " errorMsg:".length(), end).toLowerCase(java.util.Locale.ROOT);
                    StringBuilder hints = new StringBuilder();
                    if (containsAny(text, "signature", "certificate", "签名", "簽章", "签章")) hints.append("SIGNATURE ");
                    if (containsAny(text, "decrypt", "encrypt", "cipher", "解密", "加密")) hints.append("CRYPTO ");
                    if (containsAny(text, "token", "login", "auth", "expired", "登录", "登入", "过期", "過期")) hints.append("AUTH ");
                    flags = hints.length() == 0 ? "UNCLASSIFIED" : hints.toString().trim();
                }
                out.append("\nMSG_WORD_HINT=").append(flags);
            } else out.append("\nHTTP500_MESSAGE_MARKER=false");
            int count = 0, scanned = 0;
            for (StackTraceElement frame : error.getStackTrace()) {
                if (++scanned > 64) break;
                if (frame == null) continue;
                String owner = frame.getClassName(), method = frame.getMethodName();
                if (!owner.startsWith("com.intsig.") || owner.length() > 180 || method.length() > 80 ||
                        !owner.matches("[\\p{L}\\p{N}_.$]+") || !method.matches("[\\p{L}\\p{N}_$<>]+")) continue;
                out.append("\nSOURCE=").append(owner).append('.').append(method);
                if (++count == 2) break;
            }
            return out.toString();
        } catch (Throwable ignored) { return "DETAILS_UNAVAILABLE"; }
    }

    static synchronized String recentLoginDetails(int code, long now) {
        long age = now - loginTime;
        return loginDetails != null && loginCode == code && age >= 0 && age <= 5000000000L ? loginDetails : null;
    }

    /** Annotate only the existing error; process metadata is not request-correlated. */
    public static synchronized String loginMessage(String message, int code) {
        try {
            if (message == null || message.contains("CS-LOGIN-DIAG")) return message;
            String details = recentLoginDetails(code, System.nanoTime());
            return message + "\nCS-LOGIN-DIAG-V2 code=" + code +
                    (details == null ? "\nNO_RECENT_DETAIL" : "\nRecent exception; not request-correlated:\n" + details);
        } catch (Throwable ignored) { return message; }
    }

    private static synchronized void record(String event) {
        int index = count % events.length;
        events[index] = event;
        times[index] = System.nanoTime();
        count++;
    }

    public static void begin() {
        try { record("WORD_PREPARE"); } catch (Throwable ignored) { }
    }

    public static void token(String value) {
        try { record("REQ_TOKEN_PRESENT=" + (value != null && !value.isEmpty())); }
        catch (Throwable ignored) { }
    }

    public static void securityDevice(String value) {
        try { record("REQ_SECURITY_FIELD_PRESENT=" + (value != null && !value.isEmpty())); }
        catch (Throwable ignored) { }
    }

    public static void header(String value) {
        try {
            // Retain ONLY a bounded numeric error code, never arbitrary header text.
            String safe = value == null ? "MISSING" :
                    value.matches("-?[0-9]{1,8}") ? value : "NON_NUMERIC";
            record("HTTP_RESPONSE_HEADER=" + safe);
        } catch (Throwable ignored) { }
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) if (text.contains(word)) return true;
        return false;
    }

    /** Observe metadata only. Never consume the body or retain arbitrary text. */
    public static void response(Object response) {
        try {
            // Exact qualified app version's public Response.header(name, default).
            java.lang.reflect.Method getter = response.getClass().getMethod(
                    "O8ooOoo〇", String.class, String.class);
            String message = (String) getter.invoke(response, "X-IS-Error-Msg", "");
            String extension = (String) getter.invoke(response, "X-IS-Error-Code-Ext", "");
            String safeExtension = extension == null || extension.isEmpty() ? "EMPTY" :
                    extension.matches("-?[0-9]{1,8}") ? extension : "NON_NUMERIC";
            String flags;
            if (message == null || message.isEmpty()) flags = "EMPTY";
            else {
                String text = message.toLowerCase(java.util.Locale.ROOT);
                StringBuilder hints = new StringBuilder();
                if (containsAny(text, "quota", "credit", "balance", "limit", "额度", "額度", "次数", "次數")) hints.append("QUOTA ");
                if (containsAny(text, "token", "login", "auth", "expired", "登录", "登入", "过期", "過期")) hints.append("AUTH ");
                if (containsAny(text, "signature", "certificate", "签名", "簽章", "签章")) hints.append("SIGNATURE ");
                if (containsAny(text, "device", "install", "设备", "裝置", "設備")) hints.append("DEVICE ");
                flags = hints.length() == 0 ? "UNCLASSIFIED" : hints.toString().trim();
            }
            // Keyword matches are hints, never proof of the backend's reason.
            record("HTTP_ERROR_MSG_WORD_HINT=" + flags);
            record("HTTP_ERROR_EXT=" + safeExtension);
        } catch (Throwable ignored) {
            record("HTTP_ERROR_DETAILS_UNAVAILABLE");
        }
    }

    /** Only the qualified local piracy warning gets app-only caller names. */
    public static CharSequence startupMessage(CharSequence message) {
        try {
            return startupMessageWithFrames(message, Thread.currentThread().getStackTrace());
        } catch (Throwable ignored) { return message; }
    }

    static CharSequence startupMessageWithFrames(CharSequence message, StackTraceElement[] frames) {
        if (message == null) return null;
        String text = message.toString();
        boolean warning = text.startsWith("This CamScanner copy is not genuine.") ||
                ((text.contains("盜版") || text.contains("盗版")) &&
                 (text.contains("掃描全能王") || text.contains("扫描全能王")));
        if (!warning || text.contains("CS-BOOT-TRACE")) return message;
        StringBuilder out = new StringBuilder(text).append("\nCS-BOOT-TRACE (caller names only)");
        int emitted = 0;
        if (frames != null) for (StackTraceElement frame : frames) {
            if (frame == null) continue;
            String owner = frame.getClassName(), method = frame.getMethodName();
            if (!owner.startsWith("com.intsig.camscanner.") || owner.length() > 200 ||
                    method.length() > 80 || !owner.matches("[\\p{L}\\p{N}_.$]+") ||
                    !method.matches("[\\p{L}\\p{N}_$<>]+")) continue;
            out.append('\n').append(owner.substring("com.intsig.camscanner.".length()))
                    .append('.').append(method);
            if (++emitted == 4) break;
        }
        if (emitted == 0) out.append("\nCALLER_UNAVAILABLE");
        return out.toString();
    }

    public static synchronized String snapshot(String path) {
        try {
            String label = "PRESENTER".equals(path) || "IMAGE2WORD".equals(path) ||
                    "JSONDOC".equals(path) ? path : "UNKNOWN";
            StringBuilder out = new StringBuilder("Free usage limit reached.\nCS-DIAG-V3 ")
                    .append(label).append(" result=103\nRecent process events; not document-correlated:");
            long now = System.nanoTime();
            boolean found = false;
            for (int i = Math.max(0, count - events.length); i < count; i++) {
                int index = i % events.length;
                long age = (now - times[index]) / 1000000L;
                if (age < 0 || age > 120000L) continue;
                found = true;
                out.append('\n').append(age).append("ms ago: ").append(events[index]);
            }
            if (!found) out.append("\nUNKNOWN: no recent instrumented Word observation");
            return out.toString();
        } catch (Throwable ignored) {
            return "CS-DIAG-V3 result=103; observations unavailable";
        }
    }
}
