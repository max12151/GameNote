package be.technifutur.dal.report;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReportRepository extends JpaRepository<CommentReportEntity, Long> {

    boolean existsByCommentIdAndReporterId(Long commentId, Long reporterId);

    long countByStatus(ReportStatus status);

    /**
     * La file de modération, filtrée sur un état ou entière.
     * <p>
     * Le paramètre nul neutralise le filtre, comme ailleurs dans le projet : une seule
     * requête sert « les signalements en attente » et « tout l'historique », plutôt que deux
     * variantes de onze colonnes qui divergeraient au premier changement.
     */
    @Query("""
            select r.id as id,
                   r.commentId as commentId,
                   r.igdbGameId as igdbGameId,
                   r.reportedContent as reportedContent,
                   r.reason as reason,
                   r.details as details,
                   r.status as status,
                   r.createdAt as createdAt,
                   r.handledAt as handledAt,
                   reporter.id as reporterId,
                   reporter.username as reporterUsername,
                   author.id as reportedAuthorId,
                   author.username as reportedAuthorUsername,
                   handler.username as handledByUsername,
                   case when r.commentId is null then false else true end as commentStillExists
            from CommentReportEntity r
                join UserEntity reporter on reporter.id = r.reporterId
                join UserEntity author on author.id = r.reportedAuthorId
                left join UserEntity handler on handler.id = r.handledById
            where (:status is null or r.status = :status)
            order by r.createdAt desc
            """)
    List<CommentReportView> findReports(@Param("status") ReportStatus status, Pageable pageable);

    @Query("select count(r) from CommentReportEntity r where (:status is null or r.status = :status)")
    long countReports(@Param("status") ReportStatus status);

    /**
     * Signalements encore en attente visant un avis donné : quand un administrateur supprime
     * cet avis, ils sont tous clos d'un coup, y compris ceux d'autres membres.
     */
    List<CommentReportEntity> findByCommentIdAndStatus(Long commentId, ReportStatus status);

    Optional<CommentReportEntity> findByIdAndStatus(Long id, ReportStatus status);

    /**
     * Détache les signalements d'un avis sur le point d'être supprimé.
     * <p>
     * La base ferait déjà ce travail (ON DELETE SET NULL), mais Hibernate garde en mémoire
     * les entités qu'il a chargées : sans cette mise à jour explicite, une entité de
     * signalement déjà présente dans la session pourrait être réécrite avec l'ancien
     * identifiant d'avis, cette fois inexistant.
     */
    @Modifying
    @Query("update CommentReportEntity r set r.commentId = null where r.commentId = :commentId")
    int detachFromComment(@Param("commentId") Long commentId);
}
