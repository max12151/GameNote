package be.technifutur.dl.rating;

/**
 * Compteurs des quatre onglets de la bibliothèque.
 * <p>
 * Trois champs nommés plutôt qu'une carte : les onglets sont connus à l'avance, et un statut
 * jamais employé doit s'y lire zéro plutôt que d'être absent de la réponse.
 */
public record LibrarySummaryDto(long wishlist,
                                long playing,
                                long finished,
                                long total) {
}
