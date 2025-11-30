package org.dce.ed;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal EDSM client used to fetch system bodies as a fallback when
 * we don't have enough local journal data.
 *
 * This uses only public, read-only endpoints and does not require an API key.
 */
public final class EdsmClient {

    private static final String BASE_URL = "https://www.edsm.net";
    private final Gson gson = new Gson();

    public static final class EdsmBody {
        public String name;
        public int bodyId;
        public double distanceToArrival;
        public Double gravity;
        public boolean landable;
        public boolean hasBio;
        public boolean hasGeo;
        public boolean highValue;
        public String subType;
        public String atmosphereType;
        public String terraformState;
    }

    /**
     * Fetches system bodies from EDSM by system name.
     * Returns an empty list if the system is not found or any error occurs.
     */
    public List<EdsmBody> fetchSystemBodies(String systemName) {
        if (systemName == null || systemName.isEmpty()) {
            return List.of();
        }

        try {
            String encoded = URLEncoder.encode(systemName, StandardCharsets.UTF_8.name());
            String url = BASE_URL + "/api-system-v1/bodies?systemName=" + encoded + "&showInformation=0&showCoordinates=0";
            String json = httpGet(url);
            if (json == null || json.isEmpty()) {
                return List.of();
            }

            JsonObject root = gson.fromJson(json, JsonObject.class);
            if (root == null) {
                return List.of();
            }

            JsonArray bodiesArray = root.has("bodies") && root.get("bodies").isJsonArray()
                    ? root.getAsJsonArray("bodies")
                    : null;
            if (bodiesArray == null || bodiesArray.size() == 0) {
                return List.of();
            }

            List<EdsmBody> result = new ArrayList<>();
            for (JsonElement elem : bodiesArray) {
                if (!elem.isJsonObject()) {
                    continue;
                }
                JsonObject b = elem.getAsJsonObject();
                EdsmBody body = new EdsmBody();
                body.name = getString(b, "name");
                body.bodyId = b.has("bodyId") ? b.get("bodyId").getAsInt() : -1;
                body.distanceToArrival = b.has("distanceToArrival") ? b.get("distanceToArrival").getAsDouble() : Double.NaN;
                body.landable = b.has("isLandable") && b.get("isLandable").getAsBoolean();
                body.gravity = b.has("gravity") ? b.get("gravity").getAsDouble() : null;
                body.subType = getString(b, "subType");
                body.atmosphereType = getString(b, "atmosphereType");
                body.terraformState = getString(b, "terraformState");

                // Signals: check for biological/geological (if present)
                if (b.has("signals") && b.get("signals").isJsonArray()) {
                    JsonArray sigs = b.getAsJsonArray("signals");
                    for (JsonElement sElem : sigs) {
                        if (!sElem.isJsonObject()) {
                            continue;
                        }
                        JsonObject s = sElem.getAsJsonObject();
                        String type = getString(s, "type");
                        String loc = getString(s, "type_Localised");
                        String lowerType = type.toLowerCase();
                        String lowerLoc = loc.toLowerCase();
                        if (lowerType.contains("biological") || lowerLoc.contains("biological")) {
                            body.hasBio = true;
                        } else if (lowerType.contains("geological") || lowerLoc.contains("geological")) {
                            body.hasGeo = true;
                        }
                    }
                }

                // Simple "high value" heuristic similar to what we use for journal scans
                String pcLower = body.subType != null ? body.subType.toLowerCase() : "";
                String tfLower = body.terraformState != null ? body.terraformState.toLowerCase() : "";
                if (pcLower.contains("earth-like")) {
                    body.highValue = true;
                } else if (pcLower.contains("water world")) {
                    body.highValue = true;
                } else if (pcLower.contains("ammonia world")) {
                    body.highValue = true;
                } else if (tfLower.contains("terraformable")) {
                    body.highValue = true;
                }

                result.add(body);
            }

            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String getString(JsonObject obj, String member) {
        if (obj == null || member == null || !obj.has(member)) {
            return "";
        }
        JsonElement el = obj.get(member);
        if (el == null || el.isJsonNull()) {
            return "";
        }
        return el.getAsString();
    }

    private String httpGet(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(8000);
        conn.setRequestProperty("User-Agent", "EliteDangerousOverlay/1.0");

        int status = conn.getResponseCode();
        if (status != 200) {
            conn.disconnect();
            return null;
        }

        try (InputStream in = conn.getInputStream();
             InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
             BufferedReader reader = new BufferedReader(isr)) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } finally {
            conn.disconnect();
        }
    }
}
