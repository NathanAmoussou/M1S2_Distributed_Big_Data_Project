package Routes.Handlers;

import Config.AppConfig;
import Routes.RoutesUtils;
import Utils.RedisCacheService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;

public class AdminHandler implements HttpHandler {
    public AdminHandler() {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        String responseMessage;
        int statusCode = 200;

        try {
            if (path.equals("/admin/cache/status") && "GET".equalsIgnoreCase(method)) {
                boolean isEnabled = AppConfig.isEnabled();
                responseMessage = new JSONObject().put("cacheEnabled", isEnabled).toString();

            } else if (path.equals("/admin/cache/enable") && "POST".equalsIgnoreCase(method)) {
                // Active le cache
                if (!AppConfig.isEnabled()) {
                    AppConfig.setEnabled(true);
                    System.out.println("ADMIN ACTION: Redis Cache ENABLED via API.");
                }
                responseMessage = new JSONObject().put("cacheEnabled", true).put("message", "Cache enabled").toString();

            } else if (path.equals("/admin/cache/disable") && "POST".equalsIgnoreCase(method)) {
                if (AppConfig.isEnabled()) {
                    AppConfig.setEnabled(false);
                    System.out.println("ADMIN ACTION: Redis Cache DISABLED via API.");


                    try {
                        RedisCacheService.clearAll();
                        System.out.println("ADMIN ACTION: Redis Cache FLUSHED via API during disable.");
                    } catch (Exception e) {
                        System.err.println("ADMIN ACTION: Failed to flush Redis cache during disable: " + e.getMessage());
                    }

                }
                responseMessage = new JSONObject().put("cacheEnabled", false).put("message", "Cache disabled").toString();

            } else {
                statusCode = 404;
                responseMessage = new JSONObject().put("error", "Not Found or Method Not Allowed for " + path).toString();
            }

            RoutesUtils.sendResponse(exchange, statusCode, responseMessage);

        } catch (Exception e) {
            System.err.println("Error in AdminHandler for path " + path + ": " + e.getMessage());
            e.printStackTrace();
            RoutesUtils.sendErrorResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
        }
    }
}
