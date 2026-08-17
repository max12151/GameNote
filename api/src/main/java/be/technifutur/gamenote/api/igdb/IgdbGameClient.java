package be.technifutur.gamenote.api.igdb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
public class IgdbGameClient {

    private final RestTemplate restTemplate;
    private final IgdbProperties properties;
    private final IgdbTokenService tokenService;

    public IgdbGameClient(
            RestTemplate restTemplate,
            IgdbProperties properties,
            IgdbTokenService tokenService
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.tokenService = tokenService;
    }

    public List<IgdbGameResult> searchGames(
            String search,
            int limit
    ) {
        if (search == null || search.isBlank()) {
            throw new IllegalArgumentException(
                    "Le terme de recherche est obligatoire"
            );
        }

        int safeLimit = Math.min(Math.max(limit, 1), 50);

        String igdbQuery = """
            search "%s";
            fields id,name,summary,first_release_date,cover.url,
                   genres.name,platforms.name,rating,aggregated_rating,
                   involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
                   artworks.url,screenshots.url;
            limit %d;
            """
                .formatted(
                        escapeSearch(search),
                        safeLimit
                );

        HttpHeaders headers = new HttpHeaders();

        headers.set(
                "Client-ID",
                properties.getClientId()
        );

        headers.set(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + tokenService.getAccessToken()
        );

        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> request =
                new HttpEntity<>(igdbQuery, headers);

        String gamesUrl = properties.getApiUrl() + "/games";

        ResponseEntity<List<IgdbGameResult>> response =
                restTemplate.exchange(
                        gamesUrl,
                        HttpMethod.POST,
                        request,
                        new ParameterizedTypeReference<>() {
                        }
                );

        if (response.getBody() == null) {
            return List.of();
        }

        return response.getBody();
    }

    private String escapeSearch(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}