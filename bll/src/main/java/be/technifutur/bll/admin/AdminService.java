package be.technifutur.bll.admin;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.bll.report.CommentReportService;
import be.technifutur.dal.list.GameListRepository;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.user.UserAdminView;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ce qu'un administrateur peut faire sur les comptes, et les chiffres du site.
 * <p>
 * Toutes les opérations refusent de s'appliquer à celui qui les demande : un administrateur
 * qui se retire son propre rôle ou se suspend lui-même se ferme la porte, et il n'existe pas
 * d'écran pour rouvrir — seul un passage par la base ou par la liste
 * {@code gamenote.admin-usernames} le sortirait de là.
 */
@Service
public class AdminService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final int DASHBOARD_WINDOW_DAYS = 30;

    private final UserRepository userRepository;
    private final GameRatingRepository gameRatingRepository;
    private final GameListRepository gameListRepository;
    private final GameCommentService gameCommentService;
    private final CommentReportService commentReportService;

    public AdminService(UserRepository userRepository,
                        GameRatingRepository gameRatingRepository,
                        GameListRepository gameListRepository,
                        GameCommentService gameCommentService,
                        CommentReportService commentReportService) {
        this.userRepository = userRepository;
        this.gameRatingRepository = gameRatingRepository;
        this.gameListRepository = gameListRepository;
        this.gameCommentService = gameCommentService;
        this.commentReportService = commentReportService;
    }

    public List<UserAdminView> listUsers(String search, int page, int size) {
        return userRepository.findAdminUsers(search == null ? "" : search.strip(),
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
    }

    public long countUsers(String search) {
        return userRepository.countAdminUsers(search == null ? "" : search.strip());
    }

    /** Promeut ou rétrograde un compte. */
    @Transactional
    public UserEntity setRole(Long actorId, Long targetId, UserRole role) {
        UserEntity target = requireTarget(actorId, targetId, "Vous ne pouvez pas changer votre propre rôle");

        target.setRole(role);

        return userRepository.save(target);
    }

    /**
     * Suspend un compte : plus de connexion possible, donc plus rien de publié. Ce qui a déjà
     * été écrit reste en ligne — la suspension vise la personne, pas ses avis, que la
     * modération traite un par un.
     */
    @Transactional
    public UserEntity suspend(Long actorId, Long targetId, String reason) {
        UserEntity target = requireTarget(actorId, targetId, "Vous ne pouvez pas vous suspendre vous-même");

        if (target.isDeleted()) {
            throw new InvalidOperationException("Ce compte a été supprimé");
        }

        target.setSuspendedAt(OffsetDateTime.now());
        target.setSuspensionReason(reason == null || reason.isBlank() ? null : reason.strip());

        return userRepository.save(target);
    }

    @Transactional
    public UserEntity reactivate(Long actorId, Long targetId) {
        UserEntity target = requireTarget(actorId, targetId, "Ce compte est le vôtre");

        target.setSuspendedAt(null);
        target.setSuspensionReason(null);

        return userRepository.save(target);
    }

    public AdminDashboard getDashboard() {
        long users = userRepository.count();
        long active = userRepository.countBySuspendedAtIsNullAndDeletedAtIsNull();
        long suspended = userRepository.countBySuspendedAtIsNotNull();

        // Les comptes anonymisés se déduisent : total moins actifs moins suspendus.
        // L'anonymisation lève la suspension, les trois ensembles ne se recouvrent donc pas.
        long deleted = users - active - suspended;

        return new AdminDashboard(
                users,
                active,
                suspended,
                deleted,
                userRepository.countRegisteredSince(OffsetDateTime.now().minusDays(DASHBOARD_WINDOW_DAYS)),
                gameRatingRepository.countRatings(),
                gameRatingRepository.countDistinctRatedGames(),
                gameRatingRepository.findGlobalAverageRating(),
                gameCommentService.countAll(),
                gameListRepository.count(),
                commentReportService.countPending()
        );
    }

    private UserEntity requireTarget(Long actorId, Long targetId, String selfMessage) {
        if (actorId.equals(targetId)) {
            throw new InvalidOperationException(selfMessage);
        }

        return userRepository.findById(targetId)
                .orElseThrow(() -> new ResourceNotFoundException("Compte introuvable"));
    }
}
