package be.technifutur.dl.community;

import java.util.List;

/**
 * Page du classement communautaire.
 *
 * @param totalGames nombre total de jeux notés sur le site, pour que le front sache
 *                   s'il reste des pages à charger
 */
public record CommunityRankingDto(List<CommunityGameDto> games,
                                  long totalGames,
                                  int page,
                                  int size) {
}
