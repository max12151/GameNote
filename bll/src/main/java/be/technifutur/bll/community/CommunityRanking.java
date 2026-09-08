package be.technifutur.bll.community;

import be.technifutur.dal.rating.CommunityRankingView;
import java.util.List;

/**
 * Une page du classement des jeux.
 *
 * @param weights les constantes qui ont servi à l'ordre — transportées jusqu'au front pour
 *                qu'il puisse expliquer le classement plutôt que de le faire subir
 * @param query   la demande telle qu'elle a été comprise, filtres normalisés et tri retenu
 *                compris : le front peut ainsi refléter l'état réel du classement, y compris
 *                quand une valeur farfelue a été corrigée en silence
 */
public record CommunityRanking(List<CommunityRankingView> games,
                               long totalGames,
                               int page,
                               int size,
                               RankingWeights weights,
                               RankingQuery query) {
}
