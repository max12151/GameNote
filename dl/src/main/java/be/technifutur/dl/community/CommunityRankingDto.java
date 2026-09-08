package be.technifutur.dl.community;

import java.util.List;

/**
 * Page du classement communautaire.
 *
 * @param totalGames    nombre total de jeux notés sur le site, pour que le front sache
 *                      s'il reste des pages à charger
 * @param globalAverage moyenne du site, le {@code C} de la pondération bayésienne
 * @param minimumVotes  seuil de votes, le {@code m} de cette même pondération. Les deux
 *                      voyagent jusqu'au front pour qu'il puisse dire au joueur sur quoi
 *                      repose l'ordre affiché, plutôt que de le laisser deviner.
 * @param filters       les filtres et le tri tels que le serveur les a compris, après
 *                      normalisation. Le front s'aligne dessus plutôt que sur ce qu'il croit
 *                      avoir demandé : une année aberrante ou un tri inconnu sont corrigés
 *                      en silence, et l'écran doit montrer l'état réel du classement.
 */
public record CommunityRankingDto(List<CommunityGameDto> games,
                                  long totalGames,
                                  int page,
                                  int size,
                                  double globalAverage,
                                  double minimumVotes,
                                  RankingFiltersDto filters) {
}
