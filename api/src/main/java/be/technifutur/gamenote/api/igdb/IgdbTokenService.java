package be.technifutur.gamenote.api.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class IgdbTokenService {

    private final RestTemplate restTemplate;
    private final IgdbProperties properties;

    private String accessToken;
    private Instant tokenExpiration;

    public IgdbTokenService(
            RestTemplate restTemplate,
            IgdbProperties properties
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    public synchronized String getAccessToken() {
        if (tokenIsStillValid()) {
            return accessToken;
        }

        IgdbTokenResponse response = requestNewToken();

        this.accessToken = response.accessToken();

        /*
         * On retire 60 secondes pour éviter d'utiliser
         * un token juste avant son expiration.
         */
        this.tokenExpiration = Instant.now()
                .plusSeconds(Math.max(0, response.expiresIn() - 60));

        return this.accessToken;
    }

    private boolean tokenIsStillValid() {
        return accessToken != null
                && tokenExpiration != null
                && Instant.now().isBefore(tokenExpiration);
    }

    private IgdbTokenResponse requestNewToken() {
        MultiValueMap<String, String> formData =
                new LinkedMultiValueMap<>();

        formData.add("client_id", properties.getClientId());
        formData.add("client_secret", properties.getClientSecret());
        formData.add("grant_type", "client_credentials");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(formData, headers);

        IgdbTokenResponse response = restTemplate.postForObject(
                properties.getTokenUrl(),
                request,
                IgdbTokenResponse.class
        );

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException(
                    "Impossible de récupérer le token IGDB"
            );
        }

        return response;
    }

    private record IgdbTokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            long expiresIn,

            @JsonProperty("token_type")
            String tokenType
    ) {
    }
}