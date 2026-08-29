package be.technifutur.il.comment;

import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentView;
import be.technifutur.dl.comment.GameCommentDto;
import org.springframework.stereotype.Component;

@Component
public class GameCommentMapper {

    /**
     * Commentaire lu dans le fil d'un jeu. Les drapeaux {@code mine} / {@code canDelete} sont
     * calculés ici, une fois, plutôt que rejoués côté front pour chaque commentaire affiché.
     */
    public GameCommentDto toDto(GameCommentView view, Long currentUserId, boolean currentUserIsAdmin) {
        boolean mine = view.getAuthorId().equals(currentUserId);

        return new GameCommentDto(
                view.getId(),
                view.getIgdbGameId(),
                view.getContent(),
                view.getCreatedAt(),
                view.getUpdatedAt(),
                view.getAuthorId(),
                view.getAuthorUsername(),
                view.getAuthorHasAvatar(),
                view.getAuthorRating(),
                mine,
                mine || currentUserIsAdmin
        );
    }

    /** Commentaire que l'utilisateur courant vient d'écrire : il en est forcément l'auteur. */
    public GameCommentDto toOwnDto(GameCommentEntity entity,
                                   String authorUsername,
                                   boolean authorHasAvatar,
                                   Integer authorRating) {
        return new GameCommentDto(
                entity.getId(),
                entity.getIgdbGameId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUserId(),
                authorUsername,
                authorHasAvatar,
                authorRating,
                true,
                true
        );
    }
}
