package be.technifutur.bll.community;

/**
 * Les deux constantes de la pondération bayésienne du classement.
 * <p>
 * Elles ne sont pas figées dans le code : elles se recalculent sur les données réelles, si
 * bien que le classement d'un site où chaque jeu a trois votes et celui d'un site où il en
 * a trois cents se comportent tous les deux correctement, sans réglage manuel.
 *
 * @param globalAverage moyenne des moyennes de tous les jeux notés — le {@code C} de la
 *                      formule, vers lequel les jeux peu votés sont ramenés
 * @param minimumVotes  médiane du nombre de votes — le {@code m} de la formule. La médiane
 *                      plutôt que la moyenne : quelques jeux très commentés tireraient
 *                      celle-ci vers le haut et pénaliseraient tous les autres.
 */
public record RankingWeights(double globalAverage, double minimumVotes) {
}
