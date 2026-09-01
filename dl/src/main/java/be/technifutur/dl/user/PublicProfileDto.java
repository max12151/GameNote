package be.technifutur.dl.user;

import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GenreCountDto;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Profil d'un joueur tel que le voient les autres membres.
 * <p>
 * Type distinct de {@link UserDto} et non un sous-ensemble calculé au vol : l'adresse
 * e-mail n'existe pas dans cette classe, donc aucune évolution du mappage ne peut la
 * faire fuir ici par inadvertance. C'est la seule garantie qui tienne dans le temps —
 * masquer un champ à l'affichage n'en est pas une, la réponse HTTP le contiendrait.
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
                               List<RecentCommentDto> recentComments) {
}
