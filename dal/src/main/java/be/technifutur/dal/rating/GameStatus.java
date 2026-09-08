package be.technifutur.dal.rating;

/**
 * État d'un jeu dans la bibliothèque d'un joueur.
 * <p>
 * Le statut et la note sont indépendants : on peut avoir terminé un jeu sans l'avoir encore
 * noté. Seul {@link #WISHLIST} implique en pratique l'absence de note, mais rien ne
 * l'interdit — un joueur qui rejoue un titre déjà noté et le remet dans sa liste d'envies
 * n'a pas à perdre son avis pour autant.
 */
public enum GameStatus {

    /** À jouer : le jeu est dans la liste d'envies, il n'a pas encore été lancé. */
    WISHLIST,

    /** En cours. */
    PLAYING,

    /** Terminé. Statut par défaut d'un jeu qu'on note, puisqu'on le note après y avoir joué. */
    FINISHED
}
