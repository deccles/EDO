package org.dce.ed.edsm;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class EdsmClient {

	private static final boolean DEBUG_SPHERE_SYSTEMS = true;
	
    private static final String BASE_URL = "https://www.edsm.net";

    private final HttpClient client;
    private final Gson gson;

    public EdsmClient() {
        this.client = HttpClient.newHttpClient();
        this.gson = new GsonBuilder().create();
    }

    private <T> T get(String url, Class<T> clazz) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EDO-Tool")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body();

        if (body == null || body.isEmpty()) {
            return null;
        }

        return gson.fromJson(body, clazz);
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // ----------------- System-level -----------------

    public SystemResponse getSystem(String name) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-v1/system"
                + "?showId=1&showCoordinates=1&showPermit=1"
                + "&systemName=" + encode(name);
        return get(url, SystemResponse.class);
    }

    public SystemResponse[] getSystems(String... names) throws IOException, InterruptedException {
        String joined = String.join(",", names);
        String url = BASE_URL + "/api-v1/systems"
                + "?showId=1&showCoordinates=1&showPermit=1"
                + "&systemName=" + encode(joined);
        return get(url, SystemResponse[].class);
    }

    /**
     * Richer single-system view (info + primary star, still no stations).
     */
    public ShowSystemResponse showSystem(String systemName) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-v1/system"
                + "?systemName=" + encode(systemName)
                + "&showId=1"
                + "&showCoordinates=1"
                + "&showPermit=1"
                + "&showInformation=1"
                + "&showPrimaryStar=1";

        return get(url, ShowSystemResponse.class);
    }

    public SphereSystemsResponse[] sphereSystems(double x, double y, double z, int radiusLy)
            throws IOException, InterruptedException {

        // First try the official EDSM endpoint
        String url = BASE_URL + "/api-v1/sphere-systems"
                + "?x=" + x
                + "&y=" + y
                + "&z=" + z
                + "&radius=" + Math.min(radiusLy, 100)
                + "&showCoordinates=1&showId=1&showInformation=1";

        SphereSystemsResponse[] result = getSphereSystems(url);

        // EDSM sometimes returns {} instead of [] when broken, so detect "no data"
        if (result == null || result.length == 0) {
            // 1. Reverse-lookup system name from coordinates
            //    (calls your existing "getSystem" by proximity—works if EDSM knows coords)
            SystemResponse center = systemFromCoords(x, y, z);
            if (center != null && center.name != null) {
                return sphereSystemsLocal(center.name, radiusLy);
            }

            // If systemFromCoords doesn't exist, return empty
            return new SphereSystemsResponse[0];
        }

        return result;
    }

    // ----------------- Bodies -----------------

    public BodiesResponse showBodies(String systemName) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-system-v1/bodies?systemName=" + encode(systemName);
        return get(url, BodiesResponse.class);
    }

    public BodiesResponse showBodies(long systemId) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-system-v1/bodies?systemId=" + systemId;
        return get(url, BodiesResponse.class);
    }

    // ----------------- Stations (new) -----------------

    /**
     * Get information about stations in a system (not including fleet carriers).
     * https://www.edsm.net/api-system-v1/stations
     */
    public SystemStationsResponse getSystemStations(String systemName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-system-v1/stations"
                + "?systemName=" + encode(systemName);
        return get(url, SystemStationsResponse.class);
    }

    // ----------------- Traffic / deaths -----------------

    public TrafficResponse showTraffic(String systemName) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-system-v1/traffic?systemName=" + encode(systemName);
        return get(url, TrafficResponse.class);
    }

    public DeathsResponse showDeaths(String systemName) throws IOException, InterruptedException {
        String url = BASE_URL + "/api-system-v1/deaths?systemName=" + encode(systemName);
        return get(url, DeathsResponse.class);
    }

    // ----------------- Logs (system-level & commander-level) -----------------

    /**
     * System logs by system name (public, no API key).
     */
    public LogsResponse systemLogs(String apiKey, String commanderName, String systemName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-logs-v1/get-logs"
                + "?commanderName=" + encode(commanderName)
                + "&apiKey=" + encode(apiKey)
                + "&systemName=" + encode(systemName)
                + "&showId=1";

        return get(url, LogsResponse.class);
    }

    /**
     * Commander logs: requires BOTH commanderName and apiKey.
     */
    public LogsResponse getCmdrLogs(String apiKey, String commanderName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-logs-v1/get-logs"
                + "?commanderName=" + encode(commanderName)
                + "&apiKey=" + encode(apiKey)
                + "&showId=1";
        return get(url, LogsResponse.class);
    }

    /**
     * Commander last position.
     */
    public CmdrLastPositionResponse getCmdrLastPosition(String apiKey, String commanderName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-logs-v1/get-position"
                + "?commanderName=" + encode(commanderName)
                + "&apiKey=" + encode(apiKey)
                + "&showId=1&showCoordinates=1";
        return get(url, CmdrLastPositionResponse.class);
    }

    // ----------------- Commander-specific (ranks, credits) -----------------

    public CmdrRanksResponse getCmdrRanks(String apiKey, String commanderName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-commander-v1/get-ranks"
                + "?commanderName=" + encode(commanderName)
                + "&apiKey=" + encode(apiKey);
        return get(url, CmdrRanksResponse.class);
    }

    public CmdrCreditsResponse getCmdrCredits(String apiKey, String commanderName)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-commander-v1/get-credits"
                + "?commanderName=" + encode(commanderName)
                + "&apiKey=" + encode(apiKey);
        return get(url, CmdrCreditsResponse.class);
    }

    /**
     * Sphere-systems endpoint is a bit inconsistent in practice:
     * sometimes it returns a JSON array, sometimes a single object,
     * and occasionally an error object. This helper normalizes that
     * into a SphereSystemsResponse[] so callers don't have to care.
     */
    private SphereSystemsResponse[] getSphereSystems(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EDO-Tool")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body();

        if (DEBUG_SPHERE_SYSTEMS) {
            System.out.println("[EDSM] sphere-systems URL: " + url);
            System.out.println("[EDSM] sphere-systems HTTP " + resp.statusCode());
            System.out.println("[EDSM] sphere-systems raw body: " + body);
        }

        if (body == null || body.isEmpty()) {
            return new SphereSystemsResponse[0];
        }

        body = body.trim();

        // Fast-path: looks like an array already
        if (!body.isEmpty() && body.charAt(0) == '[') {
            try {
                return gson.fromJson(body, SphereSystemsResponse[].class);
            } catch (Exception e) {
                if (DEBUG_SPHERE_SYSTEMS) {
                    e.printStackTrace();
                }
                throw new IOException("Failed to parse sphere-systems array: " + e.getMessage(), e);
            }
        }

        // Otherwise parse and normalize
        try {
            JsonElement root = JsonParser.parseString(body);

            if (root.isJsonArray()) {
                JsonArray arr = root.getAsJsonArray();
                return gson.fromJson(arr, SphereSystemsResponse[].class);
            }

            if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();

                // If it has a "systems" array, prefer that.
                if (obj.has("systems") && obj.get("systems").isJsonArray()) {
                    JsonArray systems = obj.getAsJsonArray("systems");
                    return gson.fromJson(systems, SphereSystemsResponse[].class);
                }

                // If it looks like a single system (has at least name or coords or distance), wrap it.
                boolean looksLikeSystem =
                        obj.has("name") || obj.has("id") || obj.has("coords") || obj.has("distance");

                if (looksLikeSystem) {
                    SphereSystemsResponse single = gson.fromJson(obj, SphereSystemsResponse.class);
                    return new SphereSystemsResponse[]{single};
                }

                // Anything else (e.g. { "msg": "..."} ) => treat as "no systems"
                if (DEBUG_SPHERE_SYSTEMS) {
                    System.out.println("[EDSM] sphere-systems unexpected object, treating as empty: " + obj);
                }
                return new SphereSystemsResponse[0];
            }
        } catch (Exception e) {
            if (DEBUG_SPHERE_SYSTEMS) {
                e.printStackTrace();
            }
            throw new IOException("Unexpected EDSM sphere-systems response: " + body, e);
        }

        // Should not reach here, but just in case:
        if (DEBUG_SPHERE_SYSTEMS) {
            System.out.println("[EDSM] sphere-systems unknown structure, treating as empty: " + body);
        }
        return new SphereSystemsResponse[0];
    }
    /**
     * Extract a sector prefix from a full system name.
     * Example: "PLOEA EURL EU-R b49-0" -> "PLOEA EURL"
     */
    private String extractSectorPrefix(String systemName) {
        if (systemName == null) {
            return null;
        }
        String[] parts = systemName.trim().split(" ");
        if (parts.length < 2) {
            return systemName.trim();
        }
        return parts[0] + " " + parts[1]; // e.g. "PLOEA EURL"
    }

    /**
     * Query EDSM systems by name prefix.
     * Returns the raw JSON string EDSM gives us.
     */
    private String fetchSystemsByPrefix(String prefix) throws IOException, InterruptedException {
        String encoded = URLEncoder.encode(prefix, StandardCharsets.UTF_8);
        String url = BASE_URL + "/api-v1/systems"
                + "?systemName=" + encoded
                + "&showCoordinates=1&showId=1&showInformation=1";

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EDO-Tool")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return resp.body();
    }
    /**
     * Compute Euclidean distance in light-years for two system coords.
     */
    private double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.sqrt(dx*dx + dy*dy + dz*dz);
    }
    /**
     * Local sphere search using name prefix + coordinate filtering.
     * This bypasses EDSM's broken /sphere-systems endpoint.
     */
    public SphereSystemsResponse[] sphereSystemsLocal(String centerSystemName, int radiusLy)
            throws IOException, InterruptedException {

        // 1. Resolve center system coords via getSystem
        SystemResponse center = getSystem(centerSystemName);
        if (center == null || center.coords == null) {
            return new SphereSystemsResponse[0];
        }

        double cx = center.coords.x;
        double cy = center.coords.y;
        double cz = center.coords.z;

        // 2. Extract prefix (sector)
        String prefix = extractSectorPrefix(centerSystemName);
        if (prefix == null || prefix.isEmpty()) {
            return new SphereSystemsResponse[0];
        }

        // 3. Query systems with that prefix
        String raw = fetchSystemsByPrefix(prefix);
        if (raw == null || raw.isEmpty()) {
            return new SphereSystemsResponse[0];
        }

        JsonElement root = JsonParser.parseString(raw);
        if (!root.isJsonArray()) {
            return new SphereSystemsResponse[0];
        }

        JsonArray arr = root.getAsJsonArray();

        List<SphereSystemsResponse> out = new ArrayList<>();

        // 4. Convert & distance-filter locally
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) {
                continue;
            }
            JsonObject obj = el.getAsJsonObject();

            JsonObject coords = obj.getAsJsonObject("coords");
            if (coords == null) {
                continue;
            }

            double x = coords.has("x") ? coords.get("x").getAsDouble() : Double.NaN;
            double y = coords.has("y") ? coords.get("y").getAsDouble() : Double.NaN;
            double z = coords.has("z") ? coords.get("z").getAsDouble() : Double.NaN;

            if (Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z)) {
                continue;
            }

            double d = distance(cx, cy, cz, x, y, z);
            if (d > radiusLy) {
                continue;
            }

            // Convert into your SphereSystemsResponse type
            SphereSystemsResponse s = gson.fromJson(obj, SphereSystemsResponse.class);
            s.distance = d; // ensure distance is filled, if your DTO has a field for it
            out.add(s);
        }

        // 5. Sort by distance
        out.sort(Comparator.comparingDouble(ss -> ss.distance));

        return out.toArray(new SphereSystemsResponse[0]);
    }
    /**
     * Try to resolve a system name from coordinates.
     * EDSM has /api-v1/systems?x=...&y=...&z=...
     * (Documented but only returns exact matches.)
     */
    private SystemResponse systemFromCoords(double x, double y, double z)
            throws IOException, InterruptedException {

        String url = BASE_URL + "/api-v1/systems?x=" + x + "&y=" + y + "&z=" + z;

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "EDO-Tool")
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body();

        if (body == null || body.trim().isEmpty()) {
            return null;
        }

        JsonElement root = JsonParser.parseString(body);
        if (!root.isJsonArray()) {
            return null;
        }

        JsonArray arr = root.getAsJsonArray();
        if (arr.size() == 0) {
            return null;
        }

        return gson.fromJson(arr.get(0), SystemResponse.class);
    }

}
