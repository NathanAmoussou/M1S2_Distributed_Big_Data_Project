package Config;

public class AppConfig {
    public static final int CACHE_TTL = 300;
    private static boolean enabled = false;

    public static void setEnabled(boolean flag) {
        enabled = flag;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
