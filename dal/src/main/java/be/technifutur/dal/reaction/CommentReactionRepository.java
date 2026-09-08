package be.technifutur.dal.reaction;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReactionEntity, Long> {

    boolean existsByCommentIdAndUserId(Long commentId, Long userId);

    @Modifying
    @Query("delete from CommentReactionEntity r where r.commentId = :commentId and r.userId = :userId")
    int deleteReaction(@Param("commentId") Long commentId, @Param("userId") Long userId);

    long countByCommentId(Long commentId);

    /** Toutes les réactions données par un membre, effacées à la suppression de son compte. */
    @Modifying
    @Query("delete from CommentReactionEntity r where r.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * Réactions portées par un avis sur le point d'être supprimé.
     * <p>
     * La base ferait le travail seule (ON DELETE CASCADE), mais Hibernate garde en session
     * les entités qu'il a chargées : les retirer explicitement évite qu'une réaction déjà
     * chargée soit réécrite vers un avis qui n'existe plus.
     */
    @Modifying
    @Query("delete from CommentReactionEntity r where r.commentId = :commentId")
    int deleteByCommentId(@Param("commentId") Long commentId);

    /**
     * Compteurs « utile » pour tout un fil d'avis, en une requête. Un avis que personne n'a
     * marqué n'a pas de ligne : c'est à l'appelant de lire zéro.
     */
    @Query("""
            select r.commentId as commentId, count(r) as usefulCount
            from CommentReactionEntity r
            where r.commentId in :commentIds
            group by r.commentId
            """)
    List<CommentReactionCountView> countByCommentIds(@Param("commentIds") Collection<Long> commentIds);

    /**
     * Parmi ces avis, ceux que le membre a déjà marqués comme utiles — de quoi allumer les
     * bons boutons sans interroger la base une fois par avis affiché.
     */
    @Query("""
            select r.commentId
            from CommentReactionEntity r
            where r.userId = :userId and r.commentId in :commentIds
            """)
    List<Long> findReactedCommentIds(@Param("userId") Long userId,
                                     @Param("commentIds") Collection<Long> commentIds);
}
