package be.technifutur.dal.rating;

/**
 * Ligne du classement communautaire, augmentée du score qui décide de sa place.
 * <p>
 * La moyenne brute reste transportée à côté : c'est elle qu'on affiche au joueur, le score
 * pondéré n'étant qu'un critère d'ordre. Les montrer tous les deux évite qu'un jeu à 9,5
 * de moyenne classé septième passe pour une erreur.
 */
public interface CommunityRankingView extends CommunityGameView {

    /**
     * Moyenne tirée vers la moyenne du site à proportion du peu de votes reçus
     * (méthode bayésienne, cf. {@code findCommunityRanking}).
     */
    Double getWeightedRating();
}
