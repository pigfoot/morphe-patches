package app.pigfoot.diagnostics;

import org.junit.Test;
import static org.junit.Assert.*;

public class LoginBusinessResponseTest {
    private static final String URI = "https://api-cs.intsig.net/user/cs/query?emailEncrypt=PRIVATE_ACCOUNT&client_app=CamScanner_AD_1LITE%407.26.5.2609160000";

    private String deliver(Exception error, Object connection, int code, String uri) {
        WordTrace.loginHttp406(error, connection, code, uri);
        WordTrace.loginLinkedError(error, code);
        Object event = new Object();
        WordTrace.loginBind(event);
        return WordTrace.loginLinkedMessage("Unregistered", code, event);
    }

    @Test public void real201PathHasExactTransportAndSanitizedRoute() {
        String result = deliver(new Exception("PRIVATE_BODY"), new LinkedLoginTest.Headers("201"), 201, URI);
        assertTrue(result.contains("CS-LOGIN-DIAG-V9 code=201"));
        assertTrue(result.contains("HTTP_STATUS=406"));
        assertTrue(result.contains("ERROR_ORIGIN=HTTP_406_BUSINESS"));
        assertTrue(result.contains("ERROR_HEADER=201"));
        assertTrue(result.contains("QUERY_HOST_CLASS=ACCOUNT_PRIMARY_PRODUCTION"));
        assertTrue(result.contains("QUERY_EMAIL_ENCRYPT=PRESENT"));
        assertFalse(result.contains("PRIVATE_"));
        assertFalse(result.contains("https:"));
        assertFalse(result.contains("TRANSPORT_NOT_LINKED"));
    }

    @Test public void missingAndEmptyEncryptionAreNotConflated() {
        assertTrue(deliver(new Exception(), null, 201, URI.replace("emailEncrypt=PRIVATE_ACCOUNT&", "")).contains("QUERY_EMAIL_ENCRYPT=MISSING"));
        assertTrue(deliver(new Exception(), null, 201, URI.replace("PRIVATE_ACCOUNT", "")).contains("QUERY_EMAIL_ENCRYPT=EMPTY"));
        assertTrue(deliver(new Exception(), null, 201, URI + "&%65mailEncrypt=SECRET").contains("QUERY_ROUTE_REASON=DUPLICATE_EMAIL"));
    }

    @Test public void unrelatedCodesAndErrorObjectsCannotBorrow406Metadata() {
        Exception error = new Exception();
        assertTrue(deliver(error, new LinkedLoginTest.Headers("202"), 202, URI).contains("TRANSPORT_NOT_LINKED"));
        Exception observed = new Exception(), other = new Exception();
        WordTrace.loginHttp406(observed, new LinkedLoginTest.Headers("201"), 201, URI);
        WordTrace.loginLinkedError(other, 201);
        Object event = new Object(); WordTrace.loginBind(event);
        assertTrue(WordTrace.loginLinkedMessage("Error", 201, event).contains("TRANSPORT_NOT_LINKED"));
        WordTrace.loginLinkedError(observed, 201);
        Object own = new Object(); WordTrace.loginBind(own);
        assertTrue(WordTrace.loginLinkedMessage("Error", 201, own).contains("HTTP_STATUS=406"));
    }

    @Test public void reflectionFailureAndUnsafeUrlsStayBounded() {
        String result = deliver(new Exception("PRIVATE_BODY"), new Object(), 201, "https://PRIVATE:SECRET@api-cs.intsig.net/user/cs/query");
        assertTrue(result.contains("ERROR_HEADER=UNAVAILABLE"));
        assertTrue(result.contains("QUERY_ROUTE_REASON=USERINFO"));
        assertFalse(result.contains("PRIVATE"));
        assertFalse(result.contains("SECRET"));
    }
}
