package be.technifutur.bll.report;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.comment.GameCommentEntity;
import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.report.CommentReportEntity;
import be.technifutur.dal.report.CommentReportRepository;
import be.technifutur.dal.report.CommentReportView;
import be.technifutur.dal.report.ReportReason;
import be.technifutur.dal.report.ReportStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signalement d'un avis par un membre, et traitement de la file par un administrateur.
 * <p>
 * Sans cette file, la modération attend qu'un administrateur tombe par hasard sur le contenu
 * à retirer. Le signalement conserve une copie de ce qui était écrit : accepter un
 * signalement supprime l'avis, et sans cette copie la décision ne laisserait aucune trace.
 */
@Service
public class CommentReportService {

    private static final int MAX_PAGE_SIZE = 50;

    private final CommentReportRepository commentReportRepository;
    private final GameCommentRepository gameCommentRepository;
    private final GameCommentService gameCommentService;

    public CommentReportService(CommentReportRepository commentReportRepository,
                                GameCommentRepository gameCommentRepository,
                                GameCommentService gameCommentService) {
        this.commentReportRepository = commentReportRepository;
        this.gameCommentRepository = gameCommentRepository;
        this.gameCommentService = gameCommentService;
    }

    @Transactional
    public CommentReportEntity report(Long commentId, Long reporterId, ReportReason reason, String details) {
        GameCommentEntity comment = gameCommentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Avis introuvable"));

        if (comment.getUserId().equals(reporterId)) {
            throw new InvalidOperationException("Vous pouvez supprimer votre propre avis, pas le signaler");
        }

        if (commentReportRepository.existsByCommentIdAndReporterId(commentId, reporterId)) {
            throw new DuplicateResourceException("Vous avez déjà signalé cet avis");
        }

        CommentReportEntity report = new CommentReportEntity();
        report.setCommentId(commentId);
        report.setReporterId(reporterId);
        report.setReportedAuthorId(comment.getUserId());
        report.setIgdbGameId(comment.getIgdbGameId());
        // Copie prise maintenant : l'avis peut être modifié avant que la file soit traitée, et
        // c'est bien le texte signalé qui doit être jugé.
        report.setReportedContent(comment.getContent());
        report.setReason(reason);
        report.setDetails(details == null || details.isBlank() ? null : details.strip());
        report.setStatus(ReportStatus.PENDING);
        report.setCreatedAt(OffsetDateTime.now());

        return commentReportRepository.save(report);
    }

    public List<CommentReportView> getReports(ReportStatus status, int page, int size) {
        return commentReportRepository.findReports(status,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
    }

    public long countReports(ReportStatus status) {
        return commentReportRepository.countReports(status);
    }

    public long countPending() {
        return commentReportRepository.countByStatus(ReportStatus.PENDING);
    }

    /**
     * Retient le signalement : l'avis est supprimé, et tous les signalements encore en attente
     * qui le visaient sont clos d'un coup.
     * <p>
     * Traiter les autres signalements du même avis ici, plutôt que d'attendre qu'un
     * administrateur les rouvre un par un, évite une file remplie de décisions déjà prises.
     */
    @Transactional
    public void accept(Long reportId, Long adminId) {
        CommentReportEntity report = requirePending(reportId);

        Long commentId = report.getCommentId();

        if (commentId != null) {
            // Les signalements frères sont clos avant la suppression : après elle, ils ne
            // portent plus d'identifiant d'avis et ne seraient plus retrouvables.
            List<CommentReportEntity> siblings =
                    commentReportRepository.findByCommentIdAndStatus(commentId, ReportStatus.PENDING);

            for (CommentReportEntity sibling : siblings) {
                close(sibling, adminId, ReportStatus.ACCEPTED);
            }

            gameCommentService.deleteAsModerator(commentId);
        } else {
            // L'avis a déjà disparu par une autre voie — son auteur l'a retiré, ou un
            // administrateur l'a supprimé depuis la fiche du jeu. Le signalement n'en est pas
            // moins fondé : on le clôt comme retenu, sans rien avoir à supprimer.
            close(report, adminId, ReportStatus.ACCEPTED);
        }
    }

    /** Écarte le signalement : l'avis reste en place. */
    @Transactional
    public void reject(Long reportId, Long adminId) {
        close(requirePending(reportId), adminId, ReportStatus.REJECTED);
    }

    private void close(CommentReportEntity report, Long adminId, ReportStatus status) {
        report.setStatus(status);
        report.setHandledAt(OffsetDateTime.now());
        report.setHandledById(adminId);

        commentReportRepository.save(report);
    }

    private CommentReportEntity requirePending(Long reportId) {
        return commentReportRepository.findByIdAndStatus(reportId, ReportStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Signalement introuvable ou déjà traité"));
    }
}
