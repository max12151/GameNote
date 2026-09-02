package be.technifutur.bll.community;

import be.technifutur.dal.rating.CommunityRankingView;
import java.util.List;

/**
 * Une page du classement des jeux, ordonnée par moyenne pondérée.
 *
 * @param weights les constantes qui ont servi à l'ordre — transportées jusqu'au front pour
 *                qu'il puisse expliquer le classement plutôt que de le faire subir
 */
public record CommunityRanking(List<CommunityRankingView> games,
                               long totalGames,
                               int page,
                               int size,
                               RankingWeights weights) {
}
