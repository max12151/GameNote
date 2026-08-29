package be.technifutur.bll.rating;

/**
 * Comparaison des notes de l'utilisateur avec celles du reste du site, sur les jeux que
 * plusieurs personnes ont notes.
 *
 * @param averageDelta ecart moyen entre sa note et la moyenne du site : negatif s'il note
 *                     plus severement que les autres, positif s'il est plus genereux
 * @param comparedGames nombre de jeux sur lesquels la comparaison a pu etre faite
 * @param stricter     jeux ou il note au moins un demi-point en dessous de la moyenne
 * @param aligned      jeux ou il s'ecarte de moins d'un demi-point
 * @param generous     jeux ou il note au moins un demi-point au-dessus
 */
public record TasteComparison(
        double averageDelta,
        int comparedGames,
        int stricter,
        int aligned,
        int generous
) {
    public static TasteComparison empty() {
        return new TasteComparison(0, 0, 0, 0, 0);
    }
}
