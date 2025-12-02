package org.dce.ed.edsm;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class EdsmClient {

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

        String url = BASE_URL + "/api-v1/sphere-systems"
                + "?x=" + x
                + "&y=" + y
                + "&z=" + z
                + "&radius=" + radiusLy
                + "&showCoordinates=1&showId=1&showInformation=1";
        return get(url, SphereSystemsResponse[].class);
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
}
