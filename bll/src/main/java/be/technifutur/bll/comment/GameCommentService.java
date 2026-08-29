package be.technifutur.bll.comment;

import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.comment.GameCommentCountView;
import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.comment.GameCommentView;
import be.technifutur.dal.rating.GameRatingRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCommentService {

    private final GameCommentRepository gameCommentRepository;
    private final GameRatingRepository gameRatingRepository;

    public GameCommentService(GameCommentRepository gameCommentRepository,
                              GameRatingRepository gameRatingRepository) {
        this.gameCommentRepository = gameCommentRepository;
        this.gameRatingRepository = gameRatingRepository;
    }

    /**
     * Crée le commentaire de l'utilisateur sur ce jeu, ou remplace le sien s'il en a déjà un :
     * la règle "un seul commentaire par jeu et par utilisateur" est ainsi tenue sans renvoyer
     * d'erreur à quelqu'un qui veut simplement corriger son avis.
     */
    @Transactional
    public GameCommentEntity saveComment(Long userId, Long igdbGameId, String content) {
        if (!gameRatingRepository.existsByUserIdAndIgdbGameId(userId, igdbGameId)) {
            throw new InvalidOperationException("Il faut d'abord noter ce jeu pour pouvoir le commenter");
        }

        GameCommentEntity entity = gameCommentRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseGet(() -> {
                    GameCommentEntity created = new GameCommentEntity();
                    created.setUserId(userId);
                    created.setIgdbGameId(igdbGameId);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });

        entity.setContent(content.strip());

        return gameCommentRepository.save(entity);
    }

    /**
     * Supprime un commentaire. Un utilisateur ne peut retirer que le sien ; un administrateur
     * peut retirer celui de n'importe qui (modération).
     */
    @Transactional
    public void deleteComment(Long commentId, Long requesterId, boolean requesterIsAdmin) {
        GameCommentEntity comment = gameCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Commentaire introuvable"));

        if (!requesterIsAdmin && !comment.getUserId().equals(requesterId)) {
            throw new ForbiddenOperationException("Vous ne pouvez supprimer que vos propres commentaires");
        }

        gameCommentRepository.delete(comment);
    }

    /** Retire le commentaire d'un utilisateur sur un jeu, sans erreur s'il n'y en avait pas. */
    @Transactional
    public void deleteOwnComment(Long userId, Long igdbGameId) {
        gameCommentRepository.deleteByUserIdAndIgdbGameId(userId, igdbGameId);
    }

    public List<GameCommentView> getComments(Long igdbGameId) {
        return gameCommentRepository.findViewsByIgdbGameId(igdbGameId);
    }

    public Optional<GameCommentEntity> findOwnComment(Long userId, Long igdbGameId) {
        return gameCommentRepository.findByUserIdAndIgdbGameId(userId, igdbGameId);
    }

    /** Nombre de commentaires par jeu, en une requête pour toute une page de classement. */
    public Map<Long, Long> countCommentsByGame(Collection<Long> igdbGameIds) {
        if (igdbGameIds.isEmpty()) {
            return Map.of();
        }

        return gameCommentRepository.countByIgdbGameIds(igdbGameIds).stream()
                .collect(Collectors.toMap(GameCommentCountView::getIgdbGameId,
                        GameCommentCountView::getCommentCount));
    }
}
