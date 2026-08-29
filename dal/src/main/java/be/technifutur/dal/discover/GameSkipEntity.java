package be.technifutur.dal.discover;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Trace d'un jeu volontairement "passé" dans la page Découvrir.
 * <p>
 * La ligne n'est pas supprimée immédiatement : elle reste le temps du délai de réapparition
 * (cf. {@code gamenote.discover.skip-cooldown}), pendant lequel le jeu est exclu des
 * suggestions de cet utilisateur uniquement.
 */
@Entity
@Table(
        name = "game_skip",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "igdb_game_id"}),
        // Index sur (user_id, skipped_at) : la seule lecture faite sur cette table filtre
        // toujours sur ces deux colonnes ensemble ("les jeux passés récemment par X").
        indexes = @Index(name = "idx_game_skip_user_skipped_at", columnList = "user_id, skipped_at")
)
public class GameSkipEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "igdb_game_id", nullable = false)
    private Long igdbGameId;

    @Column(name = "skipped_at", nullable = false)
    private OffsetDateTime skippedAt;

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

    public OffsetDateTime getSkippedAt() {
        return skippedAt;
    }

    public void setSkippedAt(OffsetDateTime skippedAt) {
        this.skippedAt = skippedAt;
    }
}
