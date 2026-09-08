package be.technifutur.gamenote.api.igdb;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class IgdbGameClient {

    // game_type: 0 = jeu principal, 8 = remake, 9 = remaster, 10 = version enrichie avec contenu
    // additionnel significatif (ex: Persona 5 Royal) (cf. IGDB Game Type enum ; remplace l'ancien champ "category").
    // version_parent = null exclut les simples rééditions/packagings d'un jeu déjà listé (ex: Collector's
    // Edition, Anniversary Edition) qui n'apportent pas de contenu différent du jeu de base.
    private static final String BASE_WHERE = "game_type = (0,8,9,10) & version_parent = null";

    // Taille du lot de résultats pertinents demandé à IGDB avant qu'on les retrie nous-mêmes
    // par popularité (IGDB refuse de combiner "search" avec "sort" : 406 "Search is sorting
    // on relevancy and therefore sort is not applicable on search").
    private static final int SEARCH_POOL_SIZE = 50;

    // Plafond imposé par IGDB sur le paramètre "limit".
    private static final int MAX_IGDB_LIMIT = 500;

    // Champs demandés pour toutes les requêtes. La page Découvrir n'affiche qu'une
    // jaquette : lui demander en plus les artworks et les captures d'écran alourdirait
    // fortement la réponse pour des centaines de jeux, sans rien apporter à l'écran.
    private static final String DISCOVER_FIELDS = """
            id,name,summary,game_type,first_release_date,cover.url,
                   genres.name,platforms.name,rating,aggregated_rating,total_rating_count,
                   involved_companies.company.name,involved_companies.developer,involved_companies.publisher""";

    // La recherche et la fiche détaillée ajoutent les visuels au même socle, plutôt que
    // de recopier la liste : deux listes jumelles finissaient fatalement par diverger.
    private static final String FIELDS = DISCOVER_FIELDS + ",artworks.url,screenshots.url";

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

    public List<IgdbGameResult> searchGames(String search, int limit) {
        if (search == null || search.isBlank()) {
            throw new IllegalArgumentException("Le terme de recherche est obligatoire");
        }

        // On récupère un lot plus large de résultats pertinents (triés par IGDB selon la
        // pertinence textuelle), puis on les retrie nous-mêmes par popularité avant de ne
        // garder que les "safeLimit" premiers : les jeux les plus connus remontent en premier
        // sans perdre la pertinence de la recherche elle-même.
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
                        SEARCH_POOL_SIZE
                );

        return execute(igdbQuery).stream()
                .sorted(Comparator.comparingLong(this::popularity).reversed())
                .limit(clampLimit(limit))
                .toList();
    }

    private long popularity(IgdbGameResult result) {
        return result.totalRatingCount() != null ? result.totalRatingCount() : 0L;
    }

    /**
     * Les {@code poolSize} jeux les plus populaires (optionnellement d'un genre donné).
     * <p>
     * Contrairement à un affichage paginé, on ramène ici un vivier entier en une requête :
     * c'est dedans que la page Découvrir pioche au hasard, et le vivier est mis en cache
     * pour être partagé par tous les utilisateurs (cf. {@code PopularGamePool}).
     * <p>
     * {@code total_rating_count != null} écarte les jeux sans aucun vote, dont le classement
     * par popularité n'aurait aucun sens.
     */
    public List<IgdbGameResult> fetchMostPopular(String genre, int poolSize) {
        StringBuilder where = new StringBuilder(BASE_WHERE)
                .append(" & total_rating_count != null");

        if (genre != null && !genre.isBlank()) {
            where.append(" & genres.name = \"").append(escapeSearch(genre)).append('"');
        }

        String igdbQuery = """
            fields %s;
            where %s;
            sort total_rating_count desc;
            limit %d;
            """
                .formatted(
                        DISCOVER_FIELDS,
                        where,
                        Math.clamp(poolSize, 1, MAX_IGDB_LIMIT)
                );

        return execute(igdbQuery);
    }

    /**
     * Jeux pas encore sortis, du plus attendu au moins attendu.
     * <p>
     * {@code hypes} est le compteur d'attente d'IGDB (nombre de joueurs ayant ajouté le
     * titre à leur liste avant sa sortie) : c'est lui qui donne le classement, et non la
     * note, qui n'existe évidemment pas encore. On exige une jaquette et une date, sans
     * quoi la carte n'aurait rien à montrer.
     */
    public List<IgdbGameResult> fetchUpcoming(int limit) {
        long now = Instant.now().getEpochSecond();

        String igdbQuery = """
            fields %s,hypes;
            where %s & first_release_date > %d & hypes != null & cover != null;
            sort hypes desc;
            limit %d;
            """
                .formatted(
                        DISCOVER_FIELDS,
                        BASE_WHERE,
                        now,
                        Math.clamp(limit, 1, MAX_IGDB_LIMIT)
                );

        return execute(igdbQuery);
    }

    /**
     * Un jeu précis par son identifiant IGDB. Sert à présenter une fiche communautaire pour
     * un titre que personne n'a encore noté : la base du site n'en a alors aucune trace, et
     * IGDB reste la seule source de ses métadonnées.
     */
    public Optional<IgdbGameResult> fetchById(Long igdbGameId) {
        String igdbQuery = """
            fields %s;
            where id = %d;
            limit 1;
            """
                .formatted(DISCOVER_FIELDS, igdbGameId);

        return execute(igdbQuery).stream().findFirst();
    }

    private List<IgdbGameResult> execute(String igdbQuery) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Client-ID", properties.getClientId());
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getAccessToken());
        headers.setContentType(MediaType.TEXT_PLAIN);

        ResponseEntity<List<IgdbGameResult>> response = restTemplate.exchange(
                properties.getApiUrl() + "/games",
                HttpMethod.POST,
                new HttpEntity<>(igdbQuery, headers),
                new ParameterizedTypeReference<>() {
                }
        );

        // Un corps vide n'est pas une erreur : IGDB répond 200 avec un tableau absent
        // quand aucun jeu ne satisfait la requête.
        return response.getBody() != null ? response.getBody() : List.of();
    }

    private static int clampLimit(int limit) {
        return Math.clamp(limit, 1, SEARCH_POOL_SIZE);
    }

    private String escapeSearch(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
