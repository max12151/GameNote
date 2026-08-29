package be.technifutur.dl.rating;

/**
 * Situe l'utilisateur par rapport au reste du site.
 *
 * @param averageDelta ecart moyen avec la moyenne des autres joueurs : negatif s'il est
 *                     plus severe, positif s'il est plus genereux
 */
public record TasteComparisonDto(
        double averageDelta,
        int comparedGames,
        int stricter,
        int aligned,
        int generous
) {
}
