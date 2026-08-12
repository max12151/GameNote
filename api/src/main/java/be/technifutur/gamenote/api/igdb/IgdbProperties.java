package be.technifutur.gamenote.api.igdb;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IgdbProperties {
    private final String baseUrl;
    private final String clientId;
    private final String clientSecret;

    public IgdbProperties(
            @Value("${IGDB_BASE_URL:https://api.igdb.com/v4}") String baseUrl,
            @Value("${IGDB_CLIENT_ID:}") String clientId,
            @Value("${IGDB_CLIENT_SECRET:}") String clientSecret) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String baseUrl() { return baseUrl; }
    public String clientId() { return clientId; }
    public String clientSecret() { return clientSecret; }
}
