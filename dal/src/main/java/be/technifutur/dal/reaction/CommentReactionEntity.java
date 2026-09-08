package be.technifutur.dal.reaction;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * « {@code userId} a trouvé l'avis {@code commentId} utile. »
 * <p>
 * Pas de champ « type » : le geste est unique, et volontairement non conflictuel — un pouce
 * vers le bas enterrerait les avis minoritaires et ferait doublon avec le signalement. La
 * contrainte d'unicité suffit à toute la règle : le second clic retire la réaction.
 */
@Entity
@Table(
        name = "comment_reaction",
        uniqueConstraints = @UniqueConstraint(columnNames = {"comment_id", "user_id"}),
        indexes = @Index(name = "idx_comment_reaction_comment", columnList = "comment_id")
)
public class CommentReactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
