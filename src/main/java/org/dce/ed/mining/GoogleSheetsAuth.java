package org.dce.ed.mining;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.dce.ed.OverlayPreferences;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.MemoryDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;

/**
 * OAuth 2.0 for Google Sheets: run desktop flow and build credential from stored refresh token.
 */
public final class GoogleSheetsAuth {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);

    private GoogleSheetsAuth() {
    }

    /**
     * Build a credential from stored client ID, secret, and refresh token. Returns null if any are missing or transport fails.
     */
    public static Credential getCredential() {
        String clientId = OverlayPreferences.getMiningGoogleSheetsClientId();
        String clientSecret = OverlayPreferences.getMiningGoogleSheetsClientSecret();
        String refreshToken = OverlayPreferences.getMiningGoogleSheetsRefreshToken();
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()
            || refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets secrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret));
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setAccessType("offline")
                .build();
            Credential credential = new Credential.Builder(com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod())
                .setTransport(transport)
                .setJsonFactory(JSON_FACTORY)
                .setClientAuthentication(new com.google.api.client.auth.oauth2.ClientParametersAuthentication(clientId, clientSecret))
                .setTokenServerUrl(new GenericUrl("https://oauth2.googleapis.com/token"))
                .build();
            credential.setRefreshToken(refreshToken);
            credential.refreshToken();
            return credential;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Run the OAuth desktop flow: open browser, receive code, exchange for tokens, store refresh token in preferences.
     * Uses the given clientId and clientSecret (e.g. from dialog fields). Returns true if a refresh token was stored, false on cancel or error.
     */
    public static boolean runOAuthFlowAndStoreToken(String clientId, String clientSecret) {
        if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
            return false;
        }
        try {
            NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            GoogleClientSecrets secrets = new GoogleClientSecrets()
                .setInstalled(new GoogleClientSecrets.Details()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret));
            LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, secrets, SCOPES)
                .setDataStoreFactory(new MemoryDataStoreFactory())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
            Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
            String refreshToken = credential.getRefreshToken();
            if (refreshToken != null && !refreshToken.isBlank()) {
                OverlayPreferences.setMiningGoogleSheetsRefreshToken(refreshToken);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** Open the Google Cloud Console credentials page in the browser (for setup instructions). */
    public static void openCredentialsPageInBrowser() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create("https://console.cloud.google.com/apis/credentials"));
            }
        } catch (IOException ignored) {
        }
    }
}
