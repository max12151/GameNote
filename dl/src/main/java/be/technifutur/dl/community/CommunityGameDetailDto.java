package be.technifutur.dl.community;

import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.rating.GameStatusDto;
import java.util.List;

/**
 * Détail d'un jeu vu par la communauté.
 *
 * @param distribution répartition des notes (1→10), pour l'histogramme
 * @param myRating     note de l'utilisateur courant, ou null s'il n'a pas noté le jeu :
 *                     le front s'en sert pour savoir s'il peut proposer de commenter
 * @param myComment    commentaire de l'utilisateur courant, ou null
 * @param myStatus     statut du jeu dans la bibliothèque de l'utilisateur courant, ou null
 *                     s'il ne l'y a pas rangé
 * @param myListIds    identifiants de ses listes qui contiennent déjà ce jeu, pour que le
 *                     menu « ajouter à une liste » ouvre avec les bonnes cases cochées
 * @param commentSort  ordre appliqué au fil d'avis : RECENT ou USEFUL
 */
public record CommunityGameDetailDto(CommunityGameInfoDto game,
                                     double averageRating,
                                     long ratingCount,
                                     List<RatingBucketDto> distribution,
                                     List<GameCommentDto> comments,
                                     Integer myRating,
                                     GameCommentDto myComment,
                                     GameStatusDto myStatus,
                                     List<Long> myListIds,
                                     String commentSort) {
}
