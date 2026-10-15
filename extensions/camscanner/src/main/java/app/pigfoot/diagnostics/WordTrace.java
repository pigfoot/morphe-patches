package app.pigfoot.diagnostics;

/** Bounded process-local observations, NOT document-correlated network tracing. */
public final class WordTrace {
    private static final String[] events = new String[8];
    private static final long[] times = new long[8];
    private static int count;
    private WordTrace() {}

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
