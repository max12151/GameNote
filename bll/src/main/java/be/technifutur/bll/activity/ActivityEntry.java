package be.technifutur.bll.activity;

import be.technifutur.dal.activity.ActivityKind;
import be.technifutur.dal.comment.RecentGameCommentView;
import be.technifutur.dal.rating.RatingActivityView;
import java.time.OffsetDateTime;

/**
 * Une ligne du fil d'activité, quelle que soit sa nature.
 * <p>
 * Notes et avis vivent dans deux tables et se lisent par deux requêtes ; les fondre ici en un
 * seul type permet de les trier ensemble par date sans que l'appelant ait à connaître leur
 * origine. Le champ {@code content} n'est renseigné que pour un avis, {@code rating} que
 * lorsqu'une note existe.
 *
 * @param commentId identifiant de l'avis, nul pour une note — le front en a besoin pour
 *                  proposer de le marquer comme utile depuis le fil
 */
public record ActivityEntry(ActivityKind kind,
                            OffsetDateTime at,
                            Long authorId,
                            String authorUsername,
                            boolean authorHasAvatar,
                            Long igdbGameId,
                            String gameTitle,
                            String gameCoverUrl,
                            Integer rating,
                            String content,
                            Long commentId,
                            long usefulCount) {

    public static ActivityEntry ofRating(RatingActivityView view) {
        return new ActivityEntry(
                ActivityKind.RATING,
                view.getActivityAt(),
                view.getAuthorId(),
                view.getAuthorUsername(),
                view.getAuthorHasAvatar(),
                view.getIgdbGameId(),
                view.getGameTitle(),
                view.getGameCoverUrl(),
                view.getRating(),
                null,
                null,
                0
        );
    }

    public static ActivityEntry ofComment(RecentGameCommentView view) {
        return new ActivityEntry(
                ActivityKind.COMMENT,
                view.getCreatedAt(),
                view.getAuthorId(),
                view.getAuthorUsername(),
                view.getAuthorHasAvatar(),
                view.getIgdbGameId(),
                view.getGameTitle(),
                view.getGameCoverUrl(),
                view.getAuthorRating(),
                view.getContent(),
                view.getId(),
                view.getUsefulCount()
        );
    }
}
