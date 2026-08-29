package be.technifutur.dl.community;

/** Ligne du classement "Avis des joueurs". */
public record CommunityGameDto(Long igdbGameId,
                               String title,
                               String coverUrl,
                               Long releaseDate,
                               double averageRating,
                               long ratingCount,
                               long commentCount) {
}
