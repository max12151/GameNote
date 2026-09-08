package be.technifutur.dl.report;

import java.time.OffsetDateTime;

/**
 * Une ligne de la file de modération.
 *
 * @param reportedContent copie du texte au moment du signalement. C'est lui qu'on juge :
 *                        l'avis a pu être modifié depuis, ou supprimé.
 * @param commentExists   vrai tant que l'avis visé est en ligne. Faux, la décision reste
 *                        possible — le signalement était fondé même si le texte a déjà
 *                        disparu par une autre voie.
 */
public record CommentReportDto(Long id,
                               Long commentId,
                               Long igdbGameId,
                               String reportedContent,
                               ReportReasonDto reason,
                               String details,
                               ReportStatusDto status,
                               OffsetDateTime createdAt,
                               OffsetDateTime handledAt,
                               Long reporterId,
                               String reporterUsername,
                               Long reportedAuthorId,
                               String reportedAuthorUsername,
                               String handledByUsername,
                               boolean commentExists) {
}
