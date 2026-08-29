package be.technifutur.dal.comment;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Commentaire d'un joueur sur un jeu IGDB.
 * <p>
 * La contrainte d'unicité (user_id, igdb_game_id) matérialise en base la règle
 * "un seul commentaire par jeu et par utilisateur" : un second envoi met à jour
 * le commentaire existant au lieu d'en créer un nouveau.
 */
@Entity
@Table(
        name = "game_comment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "igdb_game_id"}),
        // La lecture principale est "tous les commentaires d'un jeu", d'où l'index dédié.
        indexes = @Index(name = "idx_game_comment_game", columnList = "igdb_game_id")
)
public class GameCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "igdb_game_id", nullable = false)
    private Long igdbGameId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getIgdbGameId() {
        return igdbGameId;
    }

    public void setIgdbGameId(Long igdbGameId) {
        this.igdbGameId = igdbGameId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
