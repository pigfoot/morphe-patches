package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginTraceTest {
    @Test public void errorsKeepTheirMessageAndExposeOnlyTheNumericCode() {
        assertEquals("Service unavailable\nCS-LOGIN-DIAG-V2 code=103\nNO_RECENT_DETAIL", WordTrace.loginMessage("Service unavailable", 103));
        assertEquals("Network error\nCS-LOGIN-DIAG-V2 code=-111\nNO_RECENT_DETAIL", WordTrace.loginMessage("Network error", -111));
        assertEquals("Unknown\nCS-LOGIN-DIAG-V2 code=0\nNO_RECENT_DETAIL", WordTrace.loginMessage("Unknown", 0));
        assertEquals("Unknown\nCS-LOGIN-DIAG-V2 code=2147483647\nNO_RECENT_DETAIL", WordTrace.loginMessage("Unknown", Integer.MAX_VALUE));
        assertEquals("Unknown\nCS-LOGIN-DIAG-V2 code=-2147483648\nNO_RECENT_DETAIL", WordTrace.loginMessage("Unknown", Integer.MIN_VALUE));
    }

    @Test public void nullAndAlreadyAnnotatedMessagesAreUntouched() {
        assertNull(WordTrace.loginMessage(null, 103));
        String marked = WordTrace.loginMessage("Original", 257);
        assertSame(marked, WordTrace.loginMessage(marked, 257));
    }

    @Test public void loginCodesAreNotRetainedInWordProcessEvents() {
        WordTrace.loginMessage("Error", 987654321);
        assertFalse(WordTrace.snapshot("JSONDOC").contains("987654321"));
        assertEquals("Error\nCS-LOGIN-DIAG-V2 code=257\nNO_RECENT_DETAIL", WordTrace.loginMessage("Error", 257));
    }
}
