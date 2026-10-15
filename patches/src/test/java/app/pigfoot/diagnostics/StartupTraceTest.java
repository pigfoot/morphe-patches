package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class StartupTraceTest {
    private static final String WARNING = "您在使用的是盜版掃描全能王，請勿購買任何服務。";
    private StackTraceElement app(String method) {
        return new StackTraceElement("com.intsig.camscanner.business.PirateAppControl", method,
                "/private/user/path/token-value.java", 987);
    }
    @Test public void exactWarningShowsBoundedCallerNamesWithoutSourcePaths() {
        StackTraceElement[] frames = {app("O8"), app("一"), app("two"), app("three"), app("five")};
        String result = WordTrace.startupMessageWithFrames(WARNING, frames).toString();
        assertTrue(result.startsWith(WARNING + "\nCS-BOOT-TRACE"));
        assertTrue(result.contains("business.PirateAppControl.O8"));
        assertTrue(result.contains("business.PirateAppControl.一"));
        assertFalse(result.contains("five"));
        assertFalse(result.contains("private"));
        assertFalse(result.contains("token-value"));
        assertFalse(result.contains("987"));
    }
    @Test public void normalAndOtherRiskMessagesRetainTheExactInputObject() {
        for (CharSequence input : new CharSequence[]{new StringBuilder("Normal document message"),
                "Device risk detected", "This app is a pirated copy.", "免費使用限制已到達"}) {
            assertSame(input, WordTrace.startupMessageWithFrames(input, new StackTraceElement[]{app("O8")}));
        }
        assertNull(WordTrace.startupMessage(null));
    }
    @Test public void englishAndSimplifiedWarningsAreRecognizedAndRepeatedTracingIsIgnored() {
        for (String warning : new String[]{"This CamScanner copy is not genuine. Please do not purchase any service.",
                "您在使用的是盗版扫描全能王，请勿购买任何服务。"}) {
            CharSequence result = WordTrace.startupMessageWithFrames(warning, null);
            assertTrue(result.toString().contains("CS-BOOT-TRACE"));
            assertTrue(result.toString().contains("CALLER_UNAVAILABLE"));
            assertSame(result, WordTrace.startupMessageWithFrames(result, null));
        }
    }
    @Test public void foreignAndMalformedFramesCannotAddArbitraryText() {
        StackTraceElement[] frames = {
            new StackTraceElement("other.private.Account", "secret", "hidden.java", 1),
            new StackTraceElement("com.intsig.camscanner.\nmalformed", "method", null, 1),
            new StackTraceElement("com.intsig.camscanner.Valid", "https://private.invalid/token", null, 1),
            null
        };
        String result = WordTrace.startupMessageWithFrames(WARNING, frames).toString();
        assertTrue(result.endsWith("CALLER_UNAVAILABLE"));
        assertFalse(result.contains("secret"));
        assertFalse(result.contains("malformed"));
        assertFalse(result.contains("private.invalid"));
    }
}
