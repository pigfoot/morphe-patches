package app.pigfoot.kingnet;

/**
 * Stateless push policy for the experimental platform-message filter. It reads no data,
 * records nothing and has no side effects.
 */
public final class PushFilter {
    private PushFilter() {}

    /**
     * The app marks server-broadcast platform messages (the notification box "platform
     * messages" tab) with the feature name {@code Default}. Every other feature name,
     * including a missing one, is kept.
     */
    public static boolean isPlatformNews(String featureName) {
        return "Default".equals(featureName);
    }
}
