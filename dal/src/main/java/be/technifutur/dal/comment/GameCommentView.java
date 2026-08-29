package be.technifutur.dal.comment;

import java.time.OffsetDateTime;

/**
 * Vue en lecture d'un commentaire, enrichie de son auteur et de la note que cet auteur
 * a mise au jeu. Passer par une projection permet de tout ramener en une seule requête
 * plutôt qu'un aller-retour par commentaire pour retrouver le pseudo et la note (N+1).
 */
public interface GameCommentView {

    Long getId();

    Long getIgdbGameId();

    String getContent();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

    Long getAuthorId();

    String getAuthorUsername();

    /**
     * Vrai si l'auteur a un avatar. L'image elle-même n'est pas transportée ici : elle est
     * servie par une route dédiée, que le navigateur peut mettre en cache.
     */
    boolean getAuthorHasAvatar();

    /** Note de l'auteur sur ce jeu ; null si la note a été retirée entre-temps. */
    Integer getAuthorRating();
}
