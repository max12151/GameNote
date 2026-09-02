package be.technifutur.dl.community;

/**
 * Ligne du classement « Avis des joueurs ».
 *
 * @param averageRating  moyenne brute des notes reçues, celle qu'on montre au joueur
 * @param weightedRating moyenne pondérée qui décide de la place au classement. Les deux
 *                       sont envoyées : afficher la seconde sans la première ferait
 *                       disparaître la note réelle du jeu, afficher la première sans la
 *                       seconde rendrait l'ordre incompréhensible.
 */
public record CommunityGameDto(Long igdbGameId,
                               String title,
                               String coverUrl,
                               Long releaseDate,
                               double averageRating,
                               double weightedRating,
                               long ratingCount,
                               long commentCount) {
}
