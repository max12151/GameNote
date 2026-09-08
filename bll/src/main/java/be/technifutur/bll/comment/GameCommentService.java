package be.technifutur.bll.comment;

import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.comment.GameCommentCountView;
import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.comment.GameCommentView;
import be.technifutur.dal.comment.RecentGameCommentView;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.reaction.CommentReactionRepository;
import be.technifutur.dal.report.CommentReportRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameCommentService {

    /**
     * Plafond des listes de derniers avis. Le nombre voulu vient de l'appelant, mais pas
     * sans limite : une valeur passée dans l'URL ne doit pas pouvoir demander la table
     * entière, avec la jointure sur les notes et les utilisateurs qui va avec.
     */
    private static final int MAX_RECENT = 50;

    private final GameCommentRepository gameCommentRepository;
    private final GameRatingRepository gameRatingRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final CommentReportRepository commentReportRepository;

    public GameCommentService(GameCommentRepository gameCommentRepository,
                              GameRatingRepository gameRatingRepository,
                              CommentReactionRepository commentReactionRepository,
                              CommentReportRepository commentReportRepository) {
        this.gameCommentRepository = gameCommentRepository;
        this.gameRatingRepository = gameRatingRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.commentReportRepository = commentReportRepository;
    }

    /**
     * Crée le commentaire de l'utilisateur sur ce jeu, ou remplace le sien s'il en a déjà un :
     * la règle "un seul commentaire par jeu et par utilisateur" est ainsi tenue sans renvoyer
     * d'erreur à quelqu'un qui veut simplement corriger son avis.
     * <p>
     * Commenter exige une note, et non la simple présence du jeu dans la bibliothèque :
     * depuis les statuts, un jeu peut y figurer sans avoir été joué, et un avis affiché sans
     * note à côté n'aurait rien à quoi se rattacher.
     */
    @Transactional
    public GameCommentEntity saveComment(Long userId, Long igdbGameId, String content) {
        if (!gameRatingRepository.existsRatedByUserIdAndIgdbGameId(userId, igdbGameId)) {
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

        delete(comment);
    }

    /**
     * Supprime un commentaire sur décision de modération, sans repasser par le contrôle
     * d'appartenance : la file de signalements a déjà établi qui décide.
     */
    @Transactional
    public void deleteAsModerator(Long commentId) {
        gameCommentRepository.findById(commentId).ifPresent(this::delete);
    }

    /** Retire le commentaire d'un utilisateur sur un jeu, sans erreur s'il n'y en avait pas. */
    @Transactional
    public void deleteOwnComment(Long userId, Long igdbGameId) {
        gameCommentRepository.findByUserIdAndIgdbGameId(userId, igdbGameId).ifPresent(this::delete);
    }

    /**
     * Suppression effective, avec ce qui pend au commentaire.
     * <p>
     * La base sait cascader les réactions et détacher les signalements, mais Hibernate garde
     * en session les entités qu'il a chargées : sans ces deux appels, une réaction ou un
     * signalement déjà en mémoire pourrait être réécrit vers un commentaire disparu.
     */
    private void delete(GameCommentEntity comment) {
        commentReactionRepository.deleteByCommentId(comment.getId());
        commentReportRepository.detachFromComment(comment.getId());

        gameCommentRepository.delete(comment);
    }

    /** Le fil d'avis d'un jeu, du plus récent ou du plus utile selon ce que le lecteur demande. */
    public List<GameCommentView> getComments(Long igdbGameId, CommentSort sort) {
        return gameCommentRepository.findViewsByIgdbGameId(igdbGameId,
                (sort == null ? CommentSort.RECENT : sort).name());
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

    /**
     * Derniers avis publiés sur le site, tous jeux confondus : la vitrine de l'accueil.
     */
    public List<RecentGameCommentView> getRecentComments(int limit) {
        return gameCommentRepository.findRecentViews(null, PageRequest.of(0, clamp(limit)));
    }

    /**
     * Avis d'un joueur, du plus récent au plus ancien, page par page.
     * <p>
     * Un profil très actif peut compter des centaines d'avis : les envoyer tous pour en
     * montrer dix ferait payer à chaque visite une réponse que personne ne lit.
     */
    public List<RecentGameCommentView> getCommentsByAuthor(Long authorId, int page, int size) {
        return gameCommentRepository.findRecentViews(authorId,
                PageRequest.of(Math.max(page, 0), clamp(size)));
    }

    /** Combien d'avis ce joueur a publiés, toutes pages confondues. */
    public long countCommentsByAuthor(Long authorId) {
        return gameCommentRepository.countByUserId(authorId);
    }

    /** Tableau de bord de la console : combien d'avis le site compte. */
    public long countAll() {
        return gameCommentRepository.count();
    }

    private static int clamp(int limit) {
        return Math.clamp(limit, 1, MAX_RECENT);
    }
}
