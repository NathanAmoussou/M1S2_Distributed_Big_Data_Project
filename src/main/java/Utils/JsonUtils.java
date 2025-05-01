package Utils;

import org.bson.types.ObjectId;
import org.json.JSONObject;

import java.math.BigDecimal;

public class JsonUtils {

    /**
     * Converts a value from JSON to an ObjectId.
     * Handles both ObjectId and String representations.
     * @param json The JSONObject to extract the value from.
     * @param key The key in the JSON to look for the ObjectId.
     * @return The ObjectId or a new ObjectId if not found or invalid format.
     */
    public static ObjectId getObjectId(JSONObject json, String key) {
        Object idObj = json.opt(key);

        if (idObj instanceof ObjectId) {
            return (ObjectId) idObj;
        } else if (idObj instanceof String) {
            return new ObjectId((String) idObj);
        } else if (idObj instanceof JSONObject) {
            JSONObject idJson = (JSONObject) idObj;
            if (idJson.has("$oid")) {
                return new ObjectId(idJson.getString("$oid"));
            }
        }

        System.out.println("WARN: Creating new ObjectId for key: " + key + " because it was missing or invalid in " + json);
        return new ObjectId(); // fallback, but maybe better throw exception if you want strict behavior
    }


    /**
     * Converts a value from JSON to a BigDecimal.
     * Handles both direct BigDecimal, String representation, and MongoDB's $numberDecimal format.
     * @param json The JSONObject to extract the value from.
     * @param key The key in the JSON to look for the BigDecimal.
     * @return The BigDecimal value or BigDecimal.ZERO if not found or invalid format.
     */
    public static BigDecimal getBigDecimal(JSONObject json, String key) {
        Object obj = json.opt(key);

        if (obj instanceof BigDecimal) {
            return (BigDecimal) obj; // Direct assignment if it's a BigDecimal
        } else if (obj instanceof String) {
            return new BigDecimal((String) obj); // Convert string to BigDecimal
        } else if (obj instanceof JSONObject) {
            // Handle MongoDB's $numberDecimal format
            JSONObject objJson = (JSONObject) obj;
            if (objJson.has("$numberDecimal")) {
                return new BigDecimal(objJson.getString("$numberDecimal"));
            }
        }
        return BigDecimal.ZERO; // Default value for balance
    }
}
