package be.technifutur.dal.user;

import java.time.OffsetDateTime;

/**
 * Ligne de la table des comptes dans la console d'administration.
 * <p>
 * Les compteurs viennent de sous-requêtes portées par la requête principale : une page de
 * vingt comptes coûte une requête, pas quarante-et-une.
 */
public interface UserAdminView {

    Long getId();

    String getUsername();

    String getEmail();

    UserRole getRole();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getSuspendedAt();

    String getSuspensionReason();

    OffsetDateTime getDeletedAt();

    long getRatedGames();

    long getComments();
}
