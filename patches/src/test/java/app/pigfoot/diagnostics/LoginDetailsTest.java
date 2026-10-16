package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginDetailsTest {
    @Test public void structured500RevealsOnlyWhitelistedMetadata() {
        Exception error = new Exception("https://example.invalid/query?email=PRIVATE_EMAIL&token=PRIVATE_TOKEN errorHeadCode:10010002 errorMsg:invalid signature PRIVATE_BODY headerServer:PRIVATE_HOST");
        error.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("outside.PrivateClass", "secret", "PRIVATE_FILE", 9),
            new StackTraceElement("com.intsig.tianshu.TianShuAPI", "query", "PRIVATE_FILE", 99)
        });
        String details = WordTrace.sanitizedLoginDetails(error);
        assertTrue(details.contains("PATH_HINT=QUERY"));
        assertTrue(details.contains("HTTP500_MESSAGE_MARKER=true"));
        assertTrue(details.contains("HEADER_CODE=10010002"));
        assertTrue(details.contains("MSG_WORD_HINT=SIGNATURE"));
        assertTrue(details.contains("SOURCE=com.intsig.tianshu.TianShuAPI.query"));
        for (String secret : new String[]{"PRIVATE_", "example.invalid", "https:", "outside.PrivateClass"})
            assertFalse(details.contains(secret));
    }

    @Test public void unstructuredAndInvalidHeadersDoNotInventHttpEvidence() {
        assertEquals("PATH_HINT=LOGIN_C\nHTTP500_MESSAGE_MARKER=false",
            WordTrace.sanitizedLoginDetails(new Exception("/login_c?password=PRIVATE")));
        String bad = WordTrace.sanitizedLoginDetails(new Exception("errorHeadCode:1234567890 errorMsg:PRIVATE headerServer:PRIVATE"));
        assertTrue(bad.contains("HEADER_CODE=NON_NUMERIC"));
        assertFalse(bad.contains("123456789"));
        assertFalse(bad.contains("PRIVATE"));
    }

    @Test public void unavailableAndOversizedMessagesHaveSafeFallbacks() {
        assertEquals("DETAILS_UNAVAILABLE", WordTrace.sanitizedLoginDetails(null));
        assertEquals("DETAILS_UNAVAILABLE", WordTrace.sanitizedLoginDetails(new Exception((String)null)));
        assertEquals("DETAILS_UNAVAILABLE", WordTrace.sanitizedLoginDetails(new Exception("x".repeat(32769))));
        assertEquals("DETAILS_UNAVAILABLE", WordTrace.sanitizedLoginDetails(new Exception() {
            @Override public String getMessage() { throw new IllegalStateException("PRIVATE"); }
        }));
    }

    @Test public void recentMetadataRequiresMatchingCodeAndValidTimeWindow() {
        WordTrace.loginErrorAt(new Exception("/query?PRIVATE"), 987001, 1000000L);
        assertNotNull(WordTrace.recentLoginDetails(987001, 1000000L));
        assertNotNull(WordTrace.recentLoginDetails(987001, 5001000000L));
        assertNull(WordTrace.recentLoginDetails(987001, 5001000001L));
        assertNull(WordTrace.recentLoginDetails(987001, 999999L));
        assertNull(WordTrace.recentLoginDetails(987002, 1000000L));
        assertFalse(WordTrace.snapshot("JSONDOC").contains("987001"));
    }
}
