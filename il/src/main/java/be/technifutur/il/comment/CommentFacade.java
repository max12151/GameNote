package be.technifutur.il.comment;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.reaction.CommentReactionService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.comment.CommentRequestDto;
import be.technifutur.dl.comment.GameCommentDto;
import org.springframework.stereotype.Component;

@Component
public class CommentFacade {

    private final UserService userService;
    private final GameCommentService gameCommentService;
    private final GameRatingService gameRatingService;
    private final CommentReactionService commentReactionService;
    private final GameCommentMapper gameCommentMapper;

    public CommentFacade(UserService userService,
                         GameCommentService gameCommentService,
                         GameRatingService gameRatingService,
                         CommentReactionService commentReactionService,
                         GameCommentMapper gameCommentMapper) {
        this.userService = userService;
        this.gameCommentService = gameCommentService;
        this.gameRatingService = gameRatingService;
        this.commentReactionService = commentReactionService;
        this.gameCommentMapper = gameCommentMapper;
    }

    public GameCommentDto saveComment(String username, Long igdbGameId, CommentRequestDto request) {
        UserEntity user = userService.getByUsername(username);

        GameCommentEntity saved = gameCommentService.saveComment(user.getId(), igdbGameId, request.getContent());

        Integer authorRating = gameRatingService.findRating(user.getId(), igdbGameId)
                .map(GameRatingEntity::getRating)
                .orElse(null);

        boolean hasAvatar = user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank();

        // Le compteur est relu plutôt que remis à zéro : corriger un avis déjà marqué comme
        // utile par d'autres ne doit pas effacer ce qu'ils en ont dit.
        long usefulCount = commentReactionService.countByComments(java.util.List.of(saved.getId()))
                .getOrDefault(saved.getId(), 0L);

        return gameCommentMapper.toOwnDto(saved, user.getUsername(), hasAvatar, authorRating, usefulCount);
    }

    /**
     * Supprime un commentaire par son identifiant. Le contrôle d'accès (auteur, ou
     * administrateur pour la modération) est fait dans le service à partir du rôle lu en
     * base, et non d'une information portée par le jeton, qui pourrait être périmée.
     */
    public void deleteComment(String username, Long commentId) {
        UserEntity user = userService.getByUsername(username);
        gameCommentService.deleteComment(commentId, user.getId(), user.isAdmin());
    }

    /** Marque un avis comme utile, et renvoie le nouveau compteur. */
    public long markUseful(String username, Long commentId) {
        UserEntity user = userService.getByUsername(username);

        return commentReactionService.addReaction(commentId, user.getId());
    }

    /** Retire sa marque, et renvoie le nouveau compteur. */
    public long unmarkUseful(String username, Long commentId) {
        UserEntity user = userService.getByUsername(username);

        return commentReactionService.removeReaction(commentId, user.getId());
    }
}
