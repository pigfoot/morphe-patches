package app.pigfoot.patches;
import app.pigfoot.diagnostics.WordTrace;
import org.junit.Test;
import static org.junit.Assert.*;

public class WordTraceTest {
    public static class HeaderResponse {
        private final String message, extension;
        HeaderResponse(String message, String extension) { this.message=message; this.extension=extension; }
        public String O8ooOoo〇(String name, String fallback) {
            if ("X-IS-Error-Msg".equals(name)) return message;
            if ("X-IS-Error-Code-Ext".equals(name)) return extension;
            throw new AssertionError("unexpected header");
        }
    }
    @Test public void responseMetadataEmitsOnlyFixedHintsAndNumericExtension() {
        WordTrace.response(new HeaderResponse("quota exceeded: private-person@example.test, private-token-value", "123"));
        String text=WordTrace.snapshot("JSONDOC");
        assertTrue(text.contains("HTTP_ERROR_MSG_WORD_HINT=QUOTA AUTH"));
        assertTrue(text.contains("HTTP_ERROR_EXT=123"));
        assertFalse(text.contains("private-person"));
        assertFalse(text.contains("private-token-value"));
        WordTrace.response(new HeaderResponse("unrecognized private value", "private-extension"));
        text=WordTrace.snapshot("JSONDOC");
        assertTrue(text.contains("HTTP_ERROR_MSG_WORD_HINT=UNCLASSIFIED"));
        assertTrue(text.contains("HTTP_ERROR_EXT=NON_NUMERIC"));
        assertFalse(text.contains("private-extension"));
    }
    @Test public void emptyMetadataAndReflectionFailureAreExplicitAndNonFatal() {
        WordTrace.response(new HeaderResponse(null, ""));
        assertTrue(WordTrace.snapshot("JSONDOC").contains("HTTP_ERROR_MSG_WORD_HINT=EMPTY"));
        assertTrue(WordTrace.snapshot("JSONDOC").contains("HTTP_ERROR_EXT=EMPTY"));
        WordTrace.response(new Object());
        WordTrace.response(null);
        assertTrue(WordTrace.snapshot("JSONDOC").contains("HTTP_ERROR_DETAILS_UNAVAILABLE"));
    }
    @Test public void distinguishesPathsAndNeverExposesUnexpectedHeaderText() {
        WordTrace.begin();
        WordTrace.header("103");
        assertTrue(WordTrace.snapshot("IMAGE2WORD").contains("IMAGE2WORD result=103"));
        assertTrue(WordTrace.snapshot("JSONDOC").contains("HTTP_RESPONSE_HEADER=103"));
        WordTrace.header("private-token-example");
        String result = WordTrace.snapshot("PRESENTER");
        assertFalse(result.contains("private-token-example"));
        assertTrue(result.contains("NON_NUMERIC"));
        assertTrue(result.contains("not document-correlated"));
    }
    @Test public void recordsMissingZeroAndBoundedHistoryWithoutChangingValues() {
        WordTrace.header(null);
        assertTrue(WordTrace.snapshot("IMAGE2WORD").contains("MISSING"));
        for (int i=0;i<12;i++) WordTrace.header("0");
        String result = WordTrace.snapshot("JSONDOC");
        assertFalse(result.contains("MISSING"));
        assertEquals(8, result.split("HTTP_RESPONSE_HEADER=0", -1).length-1);
        assertFalse(WordTrace.snapshot("private-document-name").contains("private-document-name"));
    }
    @Test public void concurrentObservationsRemainBoundedAndExplicitlyUncorrelated() throws Exception {
        Thread a = new Thread(() -> { for(int i=0;i<100;i++) WordTrace.header("103"); });
        Thread b = new Thread(() -> { for(int i=0;i<100;i++) WordTrace.header("0"); });
        a.start(); b.start(); a.join(); b.join();
        String result = WordTrace.snapshot("PRESENTER");
        assertEquals(8, result.split("HTTP_RESPONSE_HEADER=", -1).length-1);
        assertTrue(result.contains("Recent process events; not document-correlated"));
    }
}
