package Config;

public class AppConfig {
    public static final int CACHE_TTL_SHORT = 180;
    public static final int CACHE_TTL = 300;
    public static final int CACHE_TTL_LONG = 90000;
    private static boolean enabled = false;

    public static void setEnabled(boolean flag) {
        enabled = flag;
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
