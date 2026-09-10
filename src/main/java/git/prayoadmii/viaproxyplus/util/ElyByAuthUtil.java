/*
 * This file is part of ViaProxy - https://github.com/RaphiMC/ViaProxy
 * Copyright (C) 2021-2026 RK_01/RaphiMC and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package git.prayoadmii.viaproxyplus.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import git.prayoadmii.viaproxyplus.saves.impl.accounts.ElyByAccount;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

public class ElyByAuthUtil {

    public static final String AUTH_URL = "https://authserver.ely.by/auth/authenticate";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public static ElyByAccount authenticate(final String username, final String password, final String twoFactorCode) throws IOException {
        final String effectivePassword = (twoFactorCode == null || twoFactorCode.trim().isEmpty()) ? password : password + ":" + twoFactorCode.trim();
        final JsonObject request = new JsonObject();
        request.addProperty("username", username.trim());
        request.addProperty("password", effectivePassword);
        request.addProperty("clientToken", UUID.randomUUID().toString());
        request.addProperty("requestUser", true);

        final JsonObject response = postJson(AUTH_URL, request);
        if (response.has("error") && response.has("errorMessage")
            && "ForbiddenOperationException".equals(response.get("error").getAsString())
            && "Account protected with two factor auth.".equals(response.get("errorMessage").getAsString())) {
            throw new TwoFactorRequiredException();
        }
        if (response.has("error") && response.has("errorMessage")
            && "ForbiddenOperationException".equals(response.get("error").getAsString())
            && "Invalid credentials. Invalid username or password.".equals(response.get("errorMessage").getAsString())) {
            throw new IOException(response.get("errorMessage").getAsString());
        }
        if (response.has("error")) {
            throw new IOException(response.has("errorMessage") ? response.get("errorMessage").getAsString() : response.get("error").getAsString());
        }

        if (!response.has("accessToken") || !response.has("selectedProfile")) {
            throw new IOException("Ely.By authentication response was incomplete.");
        }

        final JsonObject profile = response.getAsJsonObject("selectedProfile");
        final String playerName = profile.get("name").getAsString();
        final String playerUuid = normalizeUuid(profile.get("id").getAsString());
        final String accessToken = response.get("accessToken").getAsString();
        return new ElyByAccount(playerName, UUID.fromString(playerUuid), accessToken, request.get("clientToken").getAsString());
    }

    private static JsonObject postJson(final String url, final JsonObject payload) throws IOException {
        final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(20))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8))
            .build();
        final HttpResponse<String> httpResponse;
        try {
            httpResponse = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ely.By authentication request was interrupted.", e);
        }
        final int statusCode = httpResponse.statusCode();
        final String responseBody = httpResponse.body();
        final JsonElement response = parseResponse(responseBody, statusCode);
        if (statusCode != 200) {
            if (!response.isJsonObject()) {
                throw new IOException("Ely.By authentication failed with HTTP status " + statusCode + ".");
            }
            return response.getAsJsonObject();
        }
        if (!response.isJsonObject()) {
            throw new IOException("Ely.By returned an invalid authentication response.");
        }
        return response.getAsJsonObject();
    }

    private static JsonElement parseResponse(final String responseBody, final int statusCode) throws IOException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IOException("Ely.By returned an empty response with HTTP status " + statusCode + ".");
        }
        try {
            return JsonParser.parseString(responseBody);
        } catch (RuntimeException e) {
            throw new IOException("Ely.By returned an invalid JSON response with HTTP status " + statusCode + ".", e);
        }
    }

    private static String normalizeUuid(final String value) {
        final String trimmed = value.replace("-", "");
        return trimmed.substring(0, 8) + "-" + trimmed.substring(8, 12) + "-" + trimmed.substring(12, 16) + "-" + trimmed.substring(16, 20) + "-" + trimmed.substring(20);
    }

    public static class TwoFactorRequiredException extends IOException {
        public TwoFactorRequiredException() {
            super("Account protected with two-factor authentication.");
        }
    }
}
