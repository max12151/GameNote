package be.technifutur.dl.community;

import java.util.List;

/**
 * Fiche descriptive d'un jeu sur la page communautaire. Ces métadonnées viennent d'IGDB et
 * sont recopiées à l'identique dans chaque note : elles ne dépendent d'aucun utilisateur.
 */
public record CommunityGameInfoDto(Long igdbGameId,
                                   String title,
                                   String coverUrl,
                                   Long releaseDate,
                                   String summary,
                                   List<String> genres,
                                   List<String> developers,
                                   List<String> publishers,
                                   List<String> platforms) {
}
