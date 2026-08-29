package be.technifutur.bll.community;

import be.technifutur.dal.rating.CommunityGameView;
import java.util.List;

/** Une page du classement des jeux par moyenne des notes du site. */
public record CommunityRanking(List<CommunityGameView> games,
                               long totalGames,
                               int page,
                               int size) {
}
