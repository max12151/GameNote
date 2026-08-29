package be.technifutur.il.comment;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.rating.GameRatingService;
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
    private final GameCommentMapper gameCommentMapper;

    public CommentFacade(UserService userService,
                         GameCommentService gameCommentService,
                         GameRatingService gameRatingService,
                         GameCommentMapper gameCommentMapper) {
        this.userService = userService;
        this.gameCommentService = gameCommentService;
        this.gameRatingService = gameRatingService;
        this.gameCommentMapper = gameCommentMapper;
    }

    public GameCommentDto saveComment(String username, Long igdbGameId, CommentRequestDto request) {
        UserEntity user = userService.getByUsername(username);

        GameCommentEntity saved = gameCommentService.saveComment(user.getId(), igdbGameId, request.getContent());

        Integer authorRating = gameRatingService.findRating(user.getId(), igdbGameId)
                .map(GameRatingEntity::getRating)
                .orElse(null);

        boolean hasAvatar = user.getAvatarUrl() != null && !user.getAvatarUrl().isBlank();

        return gameCommentMapper.toOwnDto(saved, user.getUsername(), hasAvatar, authorRating);
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
}
