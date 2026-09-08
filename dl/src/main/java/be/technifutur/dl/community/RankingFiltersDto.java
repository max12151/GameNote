package be.technifutur.dl.community;

/**
 * Les filtres et le tri effectivement appliqués, renvoyés avec la page.
 * <p>
 * Le serveur normalise ce qu'il reçoit : il corrige un intervalle d'années à l'envers, borne
 * les valeurs aberrantes et retombe sur le tri par défaut devant un nom inconnu. Le front
 * reflète cet état-là plutôt que ce qu'il croit avoir demandé.
 */
public record RankingFiltersDto(String search,
                                String genre,
                                String platform,
                                Integer yearFrom,
                                Integer yearTo,
                                String sort) {
}
