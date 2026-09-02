package be.technifutur.dal.rating;

/**
 * Poids d'un jeu dans le classement : combien de votes, pour quelle moyenne.
 * <p>
 * Sert à calculer les deux constantes de la pondération bayésienne — la moyenne du site et
 * le seuil de votes — sans rapatrier les notes elles-mêmes.
 */
public interface GameVoteView {

    long getRatingCount();

    Double getAverageRating();
}
