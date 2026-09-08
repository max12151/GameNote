package be.technifutur.bll.reaction;

import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.reaction.CommentReactionCountView;
import be.technifutur.dal.reaction.CommentReactionEntity;
import be.technifutur.dal.reaction.CommentReactionRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * « Cet avis m'a été utile ».
 * <p>
 * Un seul geste, pas de pouce vers le bas : un vote négatif enterre les avis minoritaires et
 * fait doublon avec le signalement, qui existe pour ce qui n'a rien à faire sur le site.
 */
@Service
public class CommentReactionService {

    private final CommentReactionRepository commentReactionRepository;
    private final GameCommentRepository gameCommentRepository;

    public CommentReactionService(CommentReactionRepository commentReactionRepository,
                                  GameCommentRepository gameCommentRepository) {
        this.commentReactionRepository = commentReactionRepository;
        this.gameCommentRepository = gameCommentRepository;
    }

    /**
     * Marque l'avis comme utile et renvoie le nouveau compteur.
     * <p>
     * Idempotent : marquer deux fois n'ajoute rien. C'est le front qui décide d'appeler
     * l'ajout ou le retrait selon l'état du bouton, et un double clic ne doit pas produire
     * deux lignes.
     */
    @Transactional
    public long addReaction(Long commentId, Long userId) {
        GameCommentEntity comment = gameCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));

        // Se distinguer soi-même n'aurait aucune valeur informative pour les autres lecteurs.
        if (comment.getUserId().equals(userId)) {
            throw new InvalidOperationException("On ne peut pas marquer son propre avis comme utile");
        }

        if (!commentReactionRepository.existsByCommentIdAndUserId(commentId, userId)) {
            CommentReactionEntity reaction = new CommentReactionEntity();
            reaction.setCommentId(commentId);
            reaction.setUserId(userId);
            reaction.setCreatedAt(OffsetDateTime.now());

            commentReactionRepository.save(reaction);
        }

        return commentReactionRepository.countByCommentId(commentId);
    }

    /** Retire la marque, sans erreur s'il n'y en avait pas, et renvoie le nouveau compteur. */
    @Transactional
    public long removeReaction(Long commentId, Long userId) {
        commentReactionRepository.deleteReaction(commentId, userId);

        return commentReactionRepository.countByCommentId(commentId);
    }

    /**
     * Parmi ces avis, ceux que le membre a déjà marqués — de quoi allumer les bons boutons
     * d'un fil entier sans interroger la base une fois par avis.
     */
    public Set<Long> getReactedCommentIds(Long userId, Collection<Long> commentIds) {
        if (userId == null || commentIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(commentReactionRepository.findReactedCommentIds(userId, commentIds));
    }

    /**
     * Compteurs « utile » pour tout un lot d'avis. Un avis que personne n'a marqué est absent
     * de la carte renvoyée : c'est à l'appelant de lire zéro.
     */
    public Map<Long, Long> countByComments(Collection<Long> commentIds) {
        if (commentIds.isEmpty()) {
            return Map.of();
        }

        return commentReactionRepository.countByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(CommentReactionCountView::getCommentId,
                        CommentReactionCountView::getUsefulCount));
    }
}
