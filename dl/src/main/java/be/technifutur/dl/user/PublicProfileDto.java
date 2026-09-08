package be.technifutur.dl.user;

import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.list.GameListDto;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GenreCountDto;
import be.technifutur.dl.rating.LibrarySummaryDto;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Profil d'un joueur tel que le voient les autres membres.
 * <p>
 * Type distinct de {@link UserDto} et non un sous-ensemble calculé au vol : l'adresse
 * e-mail n'existe pas dans cette classe, donc aucune évolution du mappage ne peut la
 * faire fuir ici par inadvertance. C'est la seule garantie qui tienne dans le temps —
 * masquer un champ à l'affichage n'en est pas une, la réponse HTTP le contiendrait.
 *
 * @param recentComments première page des avis du joueur — les suivantes se demandent à
 *                       {@code GET /api/users/{id}/comments}
 * @param totalComments  nombre total d'avis publiés. Sans lui, le front ne pourrait pas
 *                       distinguer « il n'y en a que dix » de « voici les dix premiers »,
 *                       et proposerait un bouton « Voir plus » qui ne ramènerait rien.
 * @param follow         compteurs d'abonnés et d'abonnements, et état du bouton « suivre »
 * @param library        répartition de sa bibliothèque : ce qu'il joue, ce qu'il veut jouer
 * @param publicLists    ses listes publiques ; les privées ne sortent jamais d'ici
 */
public record PublicProfileDto(Long id,
                               String username,
                               String bio,
                               boolean hasAvatar,
                               String role,
                               OffsetDateTime createdAt,
                               int ratedGames,
                               Double averageRating,
                               String topGenre,
                               int topGenreCount,
                               GameRatingDto bestRatedGame,
                               List<GenreCountDto> genreBreakdown,
                               List<RecentCommentDto> recentComments,
                               long totalComments,
                               FollowStatsDto follow,
                               LibrarySummaryDto library,
                               List<GameListDto> publicLists) {
}
