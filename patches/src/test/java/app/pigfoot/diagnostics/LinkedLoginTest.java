package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.atomic.AtomicReference;

public class LinkedLoginTest {
    public static class Headers {
        private final String code;
        Headers(String code) { this.code = code; }
        public String getHeaderField(String name) {
            return name.equals("X-IS-Error-Code") ? code : "PRIVATE_SERVER_MESSAGE";
        }
    }

    private static Exception failure(String caller) {
        Exception error = new Exception("https://PRIVATE_HOST/query?email=PRIVATE_ACCOUNT&token=PRIVATE_TOKEN errorHeadCode:0 errorMsg:PRIVATE_BODY headerServer:PRIVATE_SERVER");
        error.setStackTrace(new StackTraceElement[]{
            new StackTraceElement("com.intsig.tianshu.TianShuAPI", "core", "PRIVATE_FILE", 1),
            new StackTraceElement(caller, "operationLogin", "PRIVATE_FILE", 2)
        });
        return error;
    }

    private static String deliver(Exception error, String header, Object event) {
        WordTrace.loginHttp500(error, new Headers(header), 500);
        WordTrace.loginLinkedError(error, 500);
        WordTrace.loginBind(event);
        return WordTrace.loginLinkedMessage("Service unavailable", 500, event);
    }

    @Test public void distinguishesMissingEmptyZeroAndInvalidHeaders() {
        String[] inputs = {null, "", "0", "103", "PRIVATE_TOKEN", "1234567890"};
        String[] expected = {"MISSING", "EMPTY", "0", "103", "NON_NUMERIC", "NON_NUMERIC"};
        for (int i = 0; i < inputs.length; i++) {
            String result = deliver(failure("outside.Private"), inputs[i], new Object());
            assertTrue(result.contains("ERROR_OBJECT_LINKED"));
            assertTrue(result.contains("HTTP_STATUS=500"));
            assertTrue(result.contains("ERROR_HEADER=" + expected[i] + "\n"));
            assertFalse(result.contains("PRIVATE_"));
            assertFalse(result.contains("https:"));
        }
    }

    @Test public void errorsWithSameCodeAndReversedUiOrderNeverCrossAssociate() {
        Exception first = failure("com.intsig.tsapp.account.viewmodel.EmailLoginViewModel$queryEmailAccountExist$1$1");
        Exception second = failure("com.intsig.tsapp.account.util.PwdLoginCommonControl$1");
        Object firstEvent = new Object(), secondEvent = new Object();
        WordTrace.loginHttp500(first, new Headers(null), 500);
        WordTrace.loginLinkedError(first, 500);
        WordTrace.loginBind(firstEvent);
        WordTrace.loginHttp500(second, new Headers("0"), 500);
        WordTrace.loginLinkedError(second, 500);
        WordTrace.loginBind(secondEvent);
        String b = WordTrace.loginLinkedMessage("Error", 500, secondEvent);
        String a = WordTrace.loginLinkedMessage("Error", 500, firstEvent);
        assertTrue(a.contains("QUERY_CALLER=ACCOUNT_EXISTENCE"));
        assertTrue(a.contains("ERROR_HEADER=MISSING"));
        assertTrue(b.contains("QUERY_CALLER=PASSWORD_PRECHECK"));
        assertTrue(b.contains("ERROR_HEADER=0"));
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, firstEvent).contains("UNLINKED_ERROR"));
    }

    @Test public void equalButDistinctObjectsCannotBorrowMetadata() {
        Object first = new String("same"), second = new String("same");
        WordTrace.loginLinkedError(failure("outside.Private"), 500);
        WordTrace.loginBind(first);
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, second).contains("UNLINKED_ERROR"));
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, first).contains("ERROR_OBJECT_LINKED"));
    }

    @Test public void transportAndUiCanCrossThreadsWithoutSharingPendingContexts() throws Exception {
        Exception first = failure("com.intsig.tsapp.account.util.PwdLoginCommonControl$1");
        Exception second = failure("outside.Private");
        Object a = new Object(), b = new Object();
        WordTrace.loginHttp500(first, new Headers("0"), 500);
        AtomicReference<Throwable> problem = new AtomicReference<>();
        Thread one = new Thread(() -> {
            try { WordTrace.loginLinkedError(first, 500); WordTrace.loginBind(a); }
            catch (Throwable error) { problem.set(error); }
        });
        Thread two = new Thread(() -> {
            try { WordTrace.loginLinkedError(second, 500); WordTrace.loginBind(b); }
            catch (Throwable error) { problem.set(error); }
        });
        one.start(); two.start(); one.join(); two.join();
        assertNull(problem.get());
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, a).contains("HTTP_STATUS=500"));
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, b).contains("TRANSPORT_NOT_LINKED"));
        Object orphan = new Object();
        WordTrace.loginBind(orphan);
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, orphan).contains("UNLINKED_ERROR"));
    }

    @Test public void cacheGuardsWrongCodesExpiryOverflowAndSingleConsumption() {
        Object event = new Object();
        WordTrace.linkAt(event, 500, "SAFE", 100L);
        assertNull(WordTrace.takeLinkedAt(event, 103, 100L));
        assertNull(WordTrace.takeLinkedAt(event, 500, 100L));
        WordTrace.linkAt(event, 500, "SAFE", 100L);
        assertNull(WordTrace.takeLinkedAt(event, 500, 99L));
        WordTrace.linkAt(event, 500, "SAFE", 100L);
        assertNull(WordTrace.takeLinkedAt(event, 500, 30000000101L));
        WordTrace.linkAt(event, 500, "SAFE", 100L);
        assertEquals("SAFE", WordTrace.takeLinkedAt(event, 500, 30000000100L));
        WordTrace.linkAt(event, 500, "SAFE", 100L);
        Object[] retained = new Object[16];
        for (int i = 0; i < 16; i++) {
            retained[i] = new Object();
            WordTrace.linkAt(retained[i], 500, "SAFE", 100L);
        }
        assertNull(WordTrace.takeLinkedAt(event, 500, 100L));
    }

    @Test public void reflectionFailureDoesNotInventMissingHeaderOrChangeError() {
        String result = deliver(failure("outside.Private"), "0", new Object());
        assertTrue(result.startsWith("Service unavailable\nCS-LOGIN-DIAG-V8 code=500"));
        Exception error = failure("outside.Private");
        WordTrace.loginHttp500(error, new Object(), 500);
        WordTrace.loginLinkedError(error, 500);
        Object event = new Object(); WordTrace.loginBind(event);
        result = WordTrace.loginLinkedMessage("Error", 500, event);
        assertTrue(result.contains("ERROR_HEADER=UNAVAILABLE"));
        assertFalse(result.contains("ERROR_HEADER=MISSING"));
        WordTrace.loginHttp500(error, new Headers("103"), 406);
        WordTrace.loginLinkedError(error, 406);
        event = new Object(); WordTrace.loginBind(event);
        assertTrue(WordTrace.loginLinkedMessage("Error", 406, event).contains("TRANSPORT_NOT_LINKED"));
    }

    @Test public void normalNullTaggedAndUnlinkedEventsRemainSafe() {
        assertNull(WordTrace.loginLinkedMessage(null, 0, new Object()));
        String tagged = "Original CS-LOGIN-DIAG-V3";
        assertSame(tagged, WordTrace.loginLinkedMessage(tagged, 0, new Object()));
        assertTrue(WordTrace.loginLinkedMessage("Error", 0, null).contains("UNLINKED_ERROR"));
        assertEquals("UNKNOWN", WordTrace.queryCaller(null));
        assertFalse(WordTrace.snapshot("JSONDOC").contains("ERROR_HEADER"));
    }
}
