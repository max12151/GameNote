package be.technifutur.dal.report;

import java.time.OffsetDateTime;

/**
 * Une ligne de la file de modération : le signalement, qui l'a émis, qui est visé, et si
 * l'avis existe encore.
 * <p>
 * Le texte affiché vient de la copie faite au moment du signalement, jamais de l'avis
 * lui-même : celui-ci a pu être modifié depuis, ou supprimé.
 */
public interface CommentReportView {

    Long getId();

    Long getCommentId();

    Long getIgdbGameId();

    String getReportedContent();

    ReportReason getReason();

    String getDetails();

    ReportStatus getStatus();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getHandledAt();

    Long getReporterId();

    String getReporterUsername();

    Long getReportedAuthorId();

    String getReportedAuthorUsername();

    String getHandledByUsername();

    /** Vrai tant que l'avis visé n'a pas été supprimé. */
    boolean getCommentStillExists();
}
