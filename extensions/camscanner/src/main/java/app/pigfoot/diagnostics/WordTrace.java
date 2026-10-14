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

    public static void header(String value) {
        try {
            // Retain ONLY a bounded numeric error code, never arbitrary header text.
            String safe = value == null ? "MISSING" :
                    value.matches("-?[0-9]{1,8}") ? value : "NON_NUMERIC";
            record("HTTP_RESPONSE_HEADER=" + safe);
        } catch (Throwable ignored) { }
    }

    public static synchronized String snapshot(String path) {
        try {
            String label = "PRESENTER".equals(path) || "IMAGE2WORD".equals(path) ||
                    "JSONDOC".equals(path) ? path : "UNKNOWN";
            StringBuilder out = new StringBuilder("Free usage limit reached.\nCS-DIAG-V2 ")
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
            return "CS-DIAG-V2 result=103; observations unavailable";
        }
    }
}
