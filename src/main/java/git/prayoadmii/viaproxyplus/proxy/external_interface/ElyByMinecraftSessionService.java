package git.prayoadmii.viaproxyplus.proxy.external_interface;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.minecraft.MinecraftProfileTextures;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.yggdrasil.ProfileResult;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class ElyByMinecraftSessionService implements MinecraftSessionService {

    private static final String SESSION_URL = "https://authserver.ely.by/session";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build();

    @Override
    public void joinServer(final UUID profileId, final String authenticationToken, final String serverId) throws AuthenticationException {
        final JsonObject requestBody = new JsonObject();
        requestBody.addProperty("accessToken", authenticationToken);
        requestBody.addProperty("selectedProfile", profileId.toString().replace("-", ""));
        requestBody.addProperty("serverId", serverId);
        request("/join", requestBody);
    }

    @Override
    public ProfileResult hasJoinedServer(final String profileName, final String serverId, final InetAddress address) throws AuthenticationUnavailableException {
        final String query = "?username=" + encode(profileName) + "&serverId=" + encode(serverId);
        final JsonObject response = get("/hasJoined" + query);
        if (response == null || !response.has("id")) {
            return null;
        }

        final GameProfile profile = new GameProfile(UUID.fromString(normalizeUuid(response.get("id").getAsString())), profileName);
        if (response.has("properties") && response.get("properties").isJsonArray()) {
            for (final JsonElement propertyElement : response.getAsJsonArray("properties")) {
                final JsonObject property = propertyElement.getAsJsonObject();
                profile.getProperties().put(property.get("name").getAsString(), new Property(
                    property.get("name").getAsString(),
                    property.get("value").getAsString(),
                    property.has("signature") ? property.get("signature").getAsString() : null
                ));
            }
        }
        return new ProfileResult(profile);
    }

    @Override
    public Property getPackedTextures(final GameProfile profile) {
        return profile.getProperties().get("textures").stream().findFirst().orElse(null);
    }

    @Override
    public MinecraftProfileTextures unpackTextures(final Property packedTextures) {
        return MinecraftProfileTextures.EMPTY;
    }

    @Override
    public ProfileResult fetchProfile(final UUID profileId, final boolean requireSecure) {
        return null;
    }

    @Override
    public String getSecurePropertyValue(final Property property) {
        return property.value();
    }

    private static JsonObject get(final String path) throws AuthenticationUnavailableException {
        try {
            final HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder(URI.create(SESSION_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 204 || response.body().isBlank()) {
                return null;
            }
            if (response.statusCode() != 200) {
                throw new AuthenticationUnavailableException("Ely.By returned HTTP status " + response.statusCode());
            }
            return JsonParser.parseString(response.body()).getAsJsonObject();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationUnavailableException("Ely.By request was interrupted.", e);
        } catch (IOException | RuntimeException e) {
            throw new AuthenticationUnavailableException("Ely.By session request failed.", e);
        }
    }

    private static void request(final String path, final JsonObject body) throws AuthenticationException {
        try {
            final HttpResponse<String> response = HTTP_CLIENT.send(HttpRequest.newBuilder(URI.create(SESSION_URL + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 204) {
                throw new AuthenticationException("Ely.By returned HTTP status " + response.statusCode());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("Ely.By request was interrupted.", e);
        } catch (IOException e) {
            throw new AuthenticationException("Ely.By session request failed.", e);
        }
    }

    private static String encode(final String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static String normalizeUuid(final String value) {
        final String trimmed = value.replace("-", "");
        return trimmed.substring(0, 8) + "-" + trimmed.substring(8, 12) + "-" + trimmed.substring(12, 16) + "-" + trimmed.substring(16, 20) + "-" + trimmed.substring(20);
    }
}