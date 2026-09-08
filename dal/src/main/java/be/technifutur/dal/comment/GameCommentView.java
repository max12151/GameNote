package be.technifutur.dal.comment;

import java.time.OffsetDateTime;

/**
 * Vue en lecture d'un commentaire, enrichie de son auteur, de la note que cet auteur
 * a mise au jeu et du nombre de membres qui l'ont trouvé utile. Passer par une projection
 * permet de tout ramener en une seule requête plutôt qu'un aller-retour par commentaire pour
 * retrouver le pseudo, la note et les réactions (N+1).
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

    /**
     * Vrai si l'auteur a depuis supprimé son compte. Son avis reste lisible — le fil de
     * discussion ne doit pas se retrouver troué — mais son pseudo ne mène plus à un profil.
     */
    boolean getAuthorDeleted();

    /** Note de l'auteur sur ce jeu ; null si la note a été retirée entre-temps. */
    Integer getAuthorRating();

    /** Combien de membres ont marqué cet avis comme utile. */
    long getUsefulCount();
}
