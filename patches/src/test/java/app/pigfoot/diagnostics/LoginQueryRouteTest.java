package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginQueryRouteTest {
    private static final String CLIENT = "CamScanner_AD_1FULL%407.26.5.2609160000";
    private static String observe(String url) {
        return WordTrace.loginQueryRoute(url + " errorHeadCode:0 errorMsg:PRIVATE_BODY headerServer:PRIVATE_SERVER");
    }
    @Test public void qualifiedRoutesExposeOnlyKnownClientAndHostClass() {
        String result = observe("https://cs-bak.intsig.net/sync/query?emailEncrypt=PRIVATE_ACCOUNT&token=PRIVATE_TOKEN&client_app=" + CLIENT);
        assertTrue(result.contains("QUERY_HOST_CLASS=DEFAULT_PRODUCTION"));
        assertTrue(result.contains("QUERY_PATH=/sync/query"));
        assertTrue(result.contains("CLIENT_APP=CamScanner_AD_1FULL@7.26.5.2609160000"));
        assertFalse(result.contains("PRIVATE_"));
        assertTrue(observe("https://cs-bak-sandbox.intsig.net/query?client_app=" + CLIENT).contains("DEFAULT_SANDBOX"));
    }
    @Test public void unknownValuesAreRedactedNotEchoed() {
        String result = observe("https://private-host.example/query?client_app=PRIVATE_CLIENT");
        assertTrue(result.contains("QUERY_HOST_CLASS=OTHER"));
        assertTrue(result.contains("CLIENT_APP=OTHER"));
        assertFalse(result.contains("private-host"));
        assertFalse(result.contains("PRIVATE_"));
        assertTrue(observe("https://cs-bak.intsig.net/query").contains("CLIENT_APP=MISSING"));
    }
    @Test public void ambiguousOrUnsupportedUrlsFailClosed() {
        String[] urls = {"https://user:PRIVATE@cs-bak.intsig.net/query", "http://cs-bak.intsig.net/query",
            "https://cs-bak.intsig.net:444/query", "https://cs-bak.intsig.net/query#PRIVATE",
            "https://cs-bak.intsig.net/PRIVATE_PATH", "https://cs-bak.intsig.net/query?client_app=x&client_app=y",
            "https://cs-bak.intsig.net/query?client_app=x&%63lient_app=y", "https://cs-bak.intsig.net/query?client_app=%ZZ"};
        for (String url : urls) assertEquals(url, "QUERY_ROUTE=UNAVAILABLE", observe(url));
        assertEquals("QUERY_ROUTE=UNAVAILABLE", WordTrace.loginQueryRoute(null));
        assertEquals("QUERY_ROUTE=UNAVAILABLE", WordTrace.loginQueryRoute("PRIVATE"));
        assertEquals("QUERY_ROUTE=UNAVAILABLE", observe("x".repeat(32769)));
    }
    @Test public void liteDebugAndExactErrorObjectRemainLinked() {
        String result = observe("https://cs-bak.intsig.net/query?client_app=CamScanner_AD_1LITE%407.26.5.2609160000.Debug");
        assertTrue(result.contains("CLIENT_APP=CamScanner_AD_1LITE@7.26.5.2609160000.Debug"));
        Exception error = new Exception("https://cs-bak.intsig.net/query?client_app=" + CLIENT + " errorHeadCode:0 errorMsg: headerServer:");
        WordTrace.loginHttp500(error, new LinkedLoginTest.Headers(null), 500);
        WordTrace.loginLinkedError(error, 500);
        Object event = new Object(); WordTrace.loginBind(event);
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, event).contains("QUERY_HOST_CLASS=DEFAULT_PRODUCTION"));
        assertTrue(WordTrace.loginLinkedMessage("Error", 500, new Object()).contains("UNLINKED_ERROR"));
    }
}
