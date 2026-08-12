package be.technifutur.gamenote.api.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class IgdbTokenService {
    private final RestClient twitch;
    private final IgdbProperties properties;
    private volatile CachedToken cached;

    public IgdbTokenService(RestClient twitchRestClient, IgdbProperties properties) {
        this.twitch = twitchRestClient;
        this.properties = properties;
    }

    public String token() {
        if (properties.clientId().isBlank() || properties.clientSecret().isBlank()) {
            throw new IllegalStateException("IGDB_CLIENT_ID and IGDB_CLIENT_SECRET must be configured");
        }
        var current = cached;
        if (current != null && current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
            return current.value();
        }
        synchronized (this) {
            current = cached;
            if (current == null || !current.expiresAt().isAfter(Instant.now().plusSeconds(60))) {
                var response = twitch.post()
                        .uri(uri -> uri.path("/oauth2/token")
                                .queryParam("client_id", properties.clientId())
                                .queryParam("client_secret", properties.clientSecret())
                                .queryParam("grant_type", "client_credentials")
                                .build())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .retrieve()
                        .body(TokenResponse.class);
                if (response == null || response.accessToken() == null) {
                    throw new IllegalStateException("Twitch returned no IGDB access token");
                }
                cached = new CachedToken(response.accessToken(),
                        Instant.now().plusSeconds(Math.max(response.expiresIn(), 120)));
            }
            return cached.value();
        }
    }

    private record CachedToken(String value, Instant expiresAt) {}

    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("token_type") String tokenType) {}
}
