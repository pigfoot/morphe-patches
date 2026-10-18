package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginBootstrapTest {
    private static String details() { return WordTrace.bootstrapDetailsAt(System.nanoTime()); }
    @Test public void staleBackupMissThenSignedOfficialConfigIsObservableWithoutValues() {
        assertNull(WordTrace.refreshLoginApis("{\"UAPI\":\"https://api-cs-bak.intsig.net/user/cs\"}"));
        WordTrace.bootstrapBegin();
        WordTrace.bootstrapQuery("?emailEncrypt=PRIVATE_EMAIL&sign=0123456789ABCDEF0123456789ABCDEF");
        WordTrace.bootstrapResult("{\"UAPI\":\"https://api-cs-us.intsig.net/user/cs\",\"secret\":\"PRIVATE_BODY\"}");
        String d = details();
        assertTrue(d.contains("BOOT_CACHE=OBSOLETE_MISS"));
        assertTrue(d.contains("BOOT_PHASE=FETCH_RETURNED"));
        assertTrue(d.contains("BOOT_SIGN=PRESENT"));
        assertTrue(d.contains("BOOT_RESULT=US;RET=MISSING"));
        assertFalse(d.contains("PRIVATE"));
        assertFalse(d.contains("0123456789"));
    }
    @Test public void errorOnlyOrInvalidCachedJsonDoesNotMasqueradeAsFreshConfig() {
        for (String cached : new String[]{"{}", "{\"ret\":515}", "{\"UAPI\":null}"}) {
            assertSame(cached, WordTrace.refreshLoginApis(cached));
            assertTrue(details().contains("BOOT_CACHE=PRESERVED_NO_UAPI"));
            assertTrue(details().contains("BOOT_PHASE=CACHE"));
        }
        assertEquals("bad", WordTrace.refreshLoginApis("bad"));
        assertTrue(details().contains("BOOT_CACHE=PRESERVED_INVALID"));
    }
    @Test public void preparedButNeverReturnedIsNotSuccessAndEmptySignIsDistinct() {
        WordTrace.refreshLoginApis(null);
        WordTrace.bootstrapBegin();
        WordTrace.bootstrapQuery("?sign=&emailEncrypt=PRIVATE");
        assertTrue(details().contains("BOOT_PHASE=REQUEST_PREPARED"));
        assertTrue(details().contains("BOOT_SIGN=EMPTY"));
        assertTrue(details().contains("BOOT_RESULT=UNOBSERVED"));
        WordTrace.bootstrapResult("{\"ret\":515,\"err\":\"PRIVATE\"}");
        assertTrue(details().contains("BOOT_RESULT=NO_UAPI;RET=515"));
        assertFalse(details().contains("PRIVATE"));
    }
    @Test public void duplicateMalformedOversizedAndMissingSignFailClosed() {
        for (String query : new String[]{"?sign=a&sign=b", "?sign=%XX", "?sign=PRIVATE", "x".repeat(32769)}) {
            WordTrace.bootstrapBegin(); WordTrace.bootstrapQuery(query);
            assertTrue(details().contains("BOOT_SIGN=INVALID"));
            assertFalse(details().contains("PRIVATE"));
        }
        WordTrace.bootstrapBegin(); WordTrace.bootstrapQuery("?emailEncrypt=PRIVATE");
        assertTrue(details().contains("BOOT_SIGN=ABSENT"));
    }
    @Test public void resultRedactionDoesNotEchoUnknownHostsOrNonNumericReturns() {
        for (String result : new String[]{"{\"UAPI\":\"https://PRIVATE.invalid\",\"ret\":\"PRIVATE\"}",
                "{\"ret\":1234567890}", "{\"UAPI\":42}", "bad", "x".repeat(32769)}) {
            WordTrace.bootstrapBegin(); WordTrace.bootstrapResult(result);
            assertFalse(details().contains("PRIVATE"));
            assertFalse(details().contains("1234567890"));
        }
        WordTrace.bootstrapBegin(); WordTrace.bootstrapResult(null);
        assertTrue(details().contains("BOOT_RESULT=ABSENT"));
    }
    @Test public void wrongThreadExpiredAndResetContextsAreNotReused() throws Exception {
        WordTrace.bootstrapBegin();
        String[] other = new String[1];
        Thread t = new Thread(() -> other[0] = details()); t.start(); t.join();
        assertEquals("BOOT_CONTEXT=UNAVAILABLE", other[0]);
        assertEquals("BOOT_CONTEXT=UNAVAILABLE", WordTrace.bootstrapDetailsAt(System.nanoTime() + 31000000000L));
        WordTrace.refreshLoginApis(null);
        assertTrue(details().contains("BOOT_CACHE=ABSENT"));
        assertTrue(details().contains("BOOT_SIGN=UNOBSERVED"));
    }
}
