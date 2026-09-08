package be.technifutur.dal.report;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Signalement d'un avis, à destination de la console d'administration.
 * <p>
 * Le signalement conserve une copie du texte visé, l'identifiant de son auteur et le jeu
 * concerné ; son lien vers l'avis passe à {@code null} si celui-ci est supprimé. Sans cette
 * copie, accepter un signalement — donc supprimer l'avis — effacerait du même coup la trace
 * de la décision : la console n'aurait plus d'historique, seulement une file qui se vide.
 */
@Entity
@Table(
        name = "comment_report",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "reporter_id"}),
        indexes = @Index(name = "idx_comment_report_status", columnList = "status, created_at")
)
public class CommentReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nul dès que l'avis visé a été supprimé ; le reste de la ligne demeure lisible. */
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "reported_author_id", nullable = false)
    private Long reportedAuthorId;

    @Column(name = "igdb_game_id", nullable = false)
    private Long igdbGameId;

    @Column(name = "reported_content", nullable = false, columnDefinition = "text")
    private String reportedContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportReason reason;

    @Column(length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "handled_at")
    private OffsetDateTime handledAt;

    @Column(name = "handled_by_id")
    private Long handledById;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCommentId() {
        return commentId;
    }

    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }

    public Long getReporterId() {
        return reporterId;
    }

    public void setReporterId(Long reporterId) {
        this.reporterId = reporterId;
    }

    public Long getReportedAuthorId() {
        return reportedAuthorId;
    }

    public void setReportedAuthorId(Long reportedAuthorId) {
        this.reportedAuthorId = reportedAuthorId;
    }

    public Long getIgdbGameId() {
        return igdbGameId;
    }

    public void setIgdbGameId(Long igdbGameId) {
        this.igdbGameId = igdbGameId;
    }

    public String getReportedContent() {
        return reportedContent;
    }

    public void setReportedContent(String reportedContent) {
        this.reportedContent = reportedContent;
    }

    public ReportReason getReason() {
        return reason;
    }

    public void setReason(ReportReason reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public ReportStatus getStatus() {
        return status;
    }

    public void setStatus(ReportStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(OffsetDateTime handledAt) {
        this.handledAt = handledAt;
    }

    public Long getHandledById() {
        return handledById;
    }

    public void setHandledById(Long handledById) {
        this.handledById = handledById;
    }
}
