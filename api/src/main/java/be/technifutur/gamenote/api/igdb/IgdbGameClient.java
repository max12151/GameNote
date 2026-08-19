package be.technifutur.gamenote.api.igdb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class IgdbGameClient {

    // game_type: 0 = jeu principal, 8 = remake, 9 = remaster, 10 = version enrichie avec contenu
    // additionnel significatif (ex: Persona 5 Royal) (cf. IGDB Game Type enum ; remplace l'ancien champ "category").
    // version_parent = null exclut les simples rééditions/packagings d'un jeu déjà listé (ex: Collector's
    // Edition, Anniversary Edition) qui n'apportent pas de contenu différent du jeu de base.
    private static final String BASE_WHERE = "game_type = (0,8,9,10) & version_parent = null";

    private static final String FIELDS = """
            id,name,summary,game_type,first_release_date,cover.url,
                   genres.name,platforms.name,rating,aggregated_rating,
                   involved_companies.company.name,involved_companies.developer,involved_companies.publisher,
                   artworks.url,screenshots.url""";

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

        int safeLimit = clampLimit(limit);

        String igdbQuery = """
            search "%s";
            fields %s;
            where %s;
            limit %d;
            """
                .formatted(
                        escapeSearch(search),
                        FIELDS,
                        BASE_WHERE,
                        safeLimit
                );

        return execute(igdbQuery);
    }

    public List<IgdbGameResult> discoverGames(
            String genre,
            Collection<Long> excludedIgdbGameIds,
            int limit,
            int offset
    ) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(offset, 0);

        StringBuilder where = new StringBuilder(BASE_WHERE);

        if (genre != null && !genre.isBlank()) {
            where.append(" & genres.name = \"").append(escapeSearch(genre)).append('"');
        }

        if (excludedIgdbGameIds != null && !excludedIgdbGameIds.isEmpty()) {
            String ids = excludedIgdbGameIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            where.append(" & id != (").append(ids).append(')');
        }

        String igdbQuery = """
            fields %s;
            where %s;
            sort total_rating_count desc;
            limit %d;
            offset %d;
            """
                .formatted(
                        FIELDS,
                        where,
                        safeLimit,
                        safeOffset
                );

        return execute(igdbQuery);
    }

    private List<IgdbGameResult> execute(String igdbQuery) {
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

    private int clampLimit(int limit) {
        return Math.min(Math.max(limit, 1), 50);
    }

    private String escapeSearch(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
