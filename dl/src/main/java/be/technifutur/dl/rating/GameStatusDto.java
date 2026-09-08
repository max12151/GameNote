package be.technifutur.dl.rating;

/**
 * Statut d'un jeu dans la bibliothèque, tel qu'il circule sur l'API.
 * <p>
 * Reprend les valeurs de l'enum du domaine, sans le référencer : le module des DTOs ne
 * dépend pas de celui de la persistance, et le contrat de l'API ne doit pas changer parce
 * qu'une constante interne a été renommée.
 */
public enum GameStatusDto {

    /** À jouer. */
    WISHLIST,

    /** En cours. */
    PLAYING,

    /** Terminé. */
    FINISHED
}
