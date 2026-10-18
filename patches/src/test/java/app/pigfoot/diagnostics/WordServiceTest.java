package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class WordServiceTest {
    @Test public void classifiesKnownServiceAndObservesConfigWithoutReturningItsValue() {
        String[][] cases = {{"cs-bak", "BACKUP"}, {"cs", "MAIN"}, {"cs-us", "US"},
                {"cs-eu", "EU"}, {"cs-bak-sandbox", "SANDBOX_BACKUP"}, {"cs-sandbox", "SANDBOX"},
                {"d123", "DATA_NODE"}, {"other-service", "OFFICIAL_OTHER"}};
        for (String[] c : cases) {
            String value = "https://" + c[0] + ".intsig.net/sync";
            String d = WordTrace.wordServiceDetails(value, value, true);
            assertEquals("WORD_SERVICE=" + c[1] + "; GENERAL_CONFIG_AT_OBSERVATION=MATCH", d);
            assertFalse(d.contains("https"));
        }
    }
    @Test public void userinfoQueryFragmentPortAndLookalikesCannotLeakPrivateValues() {
        for (String value : new String[]{"https://PRIVATE@cs.intsig.net/sync", "https://cs.intsig.net/sync?token=PRIVATE",
                "https://cs.intsig.net/sync#PRIVATE", "https://cs.intsig.net:443/sync", "http://cs.intsig.net/sync",
                "https://cs.intsig.net.evil.invalid/sync", "https://PRIVATE.invalid/PRIVATE", "PRIVATE", "x".repeat(2049)}) {
            String d = WordTrace.wordServiceDetails(value, "PRIVATE_CONFIG", true);
            assertFalse(d.contains("PRIVATE")); assertFalse(d.contains("evil.invalid"));
            assertFalse(d.contains("WORD_SERVICE=MAIN;"));
        }
    }
    @Test public void configAbsenceDifferenceAndReflectionUnavailableAreDistinct() {
        String service = "https://cs-bak.intsig.net/sync";
        assertTrue(WordTrace.wordServiceDetails(service, null, true).endsWith("=ABSENT"));
        assertTrue(WordTrace.wordServiceDetails(service, "  ", true).endsWith("=ABSENT"));
        assertTrue(WordTrace.wordServiceDetails(service, "PRIVATE_OTHER", true).endsWith("=DIFFERENT"));
        assertTrue(WordTrace.wordServiceDetails(service, 42, true).endsWith("=INVALID"));
        assertTrue(WordTrace.wordServiceDetails(service, null, false).endsWith("=UNAVAILABLE"));
        WordTrace.wordService(service);
        assertTrue(WordTrace.snapshot("JSONDOC").contains("WORD_SERVICE=BACKUP; GENERAL_CONFIG_AT_OBSERVATION=UNAVAILABLE"));
    }
    public static class Response {
        final int status;
        public Response(int status) { this.status = status; }
        public int Oo08() { return status; }
        public String O8ooOoo〇(String name, String fallback) { return "X-IS-Error-Msg".equals(name) ? "PRIVATE_DETAIL" : ""; }
    }
    @Test public void actualHttpStatusIsSeparateFromBusinessHeaderAndOriginalObjectIsUnchanged() {
        Response response = new Response(200);
        WordTrace.response(response); WordTrace.header("103");
        String text = WordTrace.snapshot("JSONDOC");
        assertTrue(text.contains("WORD_HTTP_STATUS=200"));
        assertTrue(text.contains("HTTP_RESPONSE_HEADER=103"));
        assertFalse(text.contains("PRIVATE_DETAIL"));
        assertEquals(200, response.Oo08());
    }
    @Test public void invalidStatusAndUnknownObjectAreNonFatalAndUnavailable() {
        for (int status : new int[]{0, 99, 600, 999}) {
            WordTrace.response(new Response(status));
            assertTrue(WordTrace.snapshot("JSONDOC").contains("WORD_HTTP_STATUS=UNAVAILABLE"));
        }
        WordTrace.response(new Object()); WordTrace.response(null);
        assertTrue(WordTrace.snapshot("JSONDOC").contains("WORD_HTTP_STATUS=UNAVAILABLE"));
    }
    @Test public void completeOriginalPipelineKeepsAllEightBoundedEventsAndNoSecrets() {
        WordTrace.begin(); WordTrace.securityDevice("PRIVATE_DEVICE"); WordTrace.token("PRIVATE_TOKEN");
        WordTrace.wordService("https://cs-bak.intsig.net/sync");
        WordTrace.response(new Response(200)); WordTrace.header("103");
        String text = WordTrace.snapshot("JSONDOC");
        assertTrue(text.contains("CS-DIAG-V4 JSONDOC"));
        for (String event : new String[]{"WORD_PREPARE", "REQ_SECURITY_FIELD_PRESENT=true", "REQ_TOKEN_PRESENT=true",
                "WORD_SERVICE=BACKUP", "WORD_HTTP_STATUS=200", "HTTP_ERROR_MSG_WORD_HINT=UNCLASSIFIED",
                "HTTP_ERROR_EXT=EMPTY", "HTTP_RESPONSE_HEADER=103"}) assertTrue(text, text.contains(event));
        assertFalse(text.contains("PRIVATE"));
        assertTrue(text.contains("not document-correlated"));
    }
}
