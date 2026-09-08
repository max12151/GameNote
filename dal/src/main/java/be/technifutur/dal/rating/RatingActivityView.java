package be.technifutur.dal.rating;

import java.time.OffsetDateTime;

/**
 * Une note attribuée par un joueur suivi, telle qu'elle apparaît dans le fil d'activité.
 * <p>
 * L'avatar n'est représenté que par un booléen, comme dans les vues de commentaires : une
 * image en base64 recopiée dans chaque ligne du fil ferait exploser la réponse, alors que la
 * route dédiée la sert avec un ETag et laisse le navigateur la mettre en cache.
 */
public interface RatingActivityView {

    Long getId();

    Long getIgdbGameId();

    String getGameTitle();

    String getGameCoverUrl();

    Integer getRating();

    /**
     * Date à retenir pour le fil : celle de la dernière modification, à défaut celle de la
     * création. Un jeu noté aujourd'hui après avoir attendu un an dans la liste d'envies est
     * une nouvelle d'aujourd'hui, pas d'il y a un an.
     */
    OffsetDateTime getActivityAt();

    Long getAuthorId();

    String getAuthorUsername();

    boolean getAuthorHasAvatar();
}
