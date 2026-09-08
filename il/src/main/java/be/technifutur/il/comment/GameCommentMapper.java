package be.technifutur.il.comment;

import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentView;
import be.technifutur.dal.comment.RecentGameCommentView;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.comment.RecentCommentDto;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GameCommentMapper {

    /**
     * Commentaire lu dans le fil d'un jeu. Les drapeaux {@code mine} / {@code canDelete} /
     * {@code canReact} sont calculés ici, une fois, plutôt que rejoués côté front pour chaque
     * commentaire affiché.
     *
     * @param reactedCommentIds avis que l'utilisateur courant a déjà marqués comme utiles,
     *                          ramenés en une requête pour tout le fil
     */
    public GameCommentDto toDto(GameCommentView view,
                                Long currentUserId,
                                boolean currentUserIsAdmin,
                                Set<Long> reactedCommentIds) {
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
                view.getAuthorDeleted(),
                view.getAuthorRating(),
                mine,
                mine || currentUserIsAdmin,
                view.getUsefulCount(),
                reactedCommentIds.contains(view.getId()),
                !mine
        );
    }

    /**
     * Commentaire que l'utilisateur courant vient d'écrire : il en est forcément l'auteur, ne
     * peut donc pas le marquer utile, et personne n'a encore eu le temps de le faire.
     */
    public GameCommentDto toOwnDto(GameCommentEntity entity,
                                   String authorUsername,
                                   boolean authorHasAvatar,
                                   Integer authorRating,
                                   long usefulCount) {
        return new GameCommentDto(
                entity.getId(),
                entity.getIgdbGameId(),
                entity.getContent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getUserId(),
                authorUsername,
                authorHasAvatar,
                false,
                authorRating,
                true,
                true,
                usefulCount,
                false,
                false
        );
    }

    /**
     * Commentaire affiché hors de la fiche de son jeu : le jeu devient une information à
     * montrer, et les droits de modération disparaissent — on ne supprime rien depuis un
     * flux, on y clique pour rejoindre la fiche.
     */
    public RecentCommentDto toRecentDto(RecentGameCommentView view) {
        return new RecentCommentDto(
                view.getId(),
                view.getIgdbGameId(),
                view.getGameTitle(),
                view.getGameCoverUrl(),
                view.getContent(),
                view.getCreatedAt(),
                view.getAuthorId(),
                view.getAuthorUsername(),
                view.getAuthorHasAvatar(),
                view.getAuthorDeleted(),
                view.getAuthorRating(),
                view.getUsefulCount()
        );
    }
}
