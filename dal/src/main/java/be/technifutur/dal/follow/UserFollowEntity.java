package be.technifutur.dal.follow;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * « {@code followerId} suit {@code followedId} ».
 * <p>
 * Relation orientée et sans réciprocité : suivre quelqu'un ne demande pas son accord et ne
 * crée pas le lien inverse. La contrainte d'unicité rend l'opération idempotente — cliquer
 * deux fois sur « suivre » ne crée pas deux lignes.
 */
@Entity
@Table(
        name = "user_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followed_id"}),
        indexes = @Index(name = "idx_user_follow_followed", columnList = "followed_id")
)
public class UserFollowEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follower_id", nullable = false)
    private Long followerId;

    @Column(name = "followed_id", nullable = false)
    private Long followedId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Long followerId) {
        this.followerId = followerId;
    }

    public Long getFollowedId() {
        return followedId;
    }

    public void setFollowedId(Long followedId) {
        this.followedId = followedId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
