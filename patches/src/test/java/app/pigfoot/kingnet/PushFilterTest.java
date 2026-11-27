package app.pigfoot.kingnet;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PushFilterTest {
    @Test public void onlyTheExactDefaultFeatureNameIsPlatformNews() {
        assertTrue(PushFilter.isPlatformNews("Default"));
        for (String keep : new String[] {null, "", "default", "DEFAULT", "Default ", " Default", "Msg", "Ann",
                "CommunityPushNotification", "communityMain", "serviceOrderDetail", "vote", "LS_A", "index"}) {
            assertFalse(String.valueOf(keep), PushFilter.isPlatformNews(keep));
        }
    }
}
