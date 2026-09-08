package be.technifutur.dal.list;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Liste personnalisée d'un membre : « Mon top 10 2026 », « Les meilleurs metroidvanias ».
 * <p>
 * Les éléments ne sont pas une collection JPA de cette entité mais une table à part avec son
 * propre repository : une liste peut compter des dizaines de jeux, et la page « mes listes »
 * n'affiche que des noms et des compteurs — la charger avec tous ses éléments ne servirait
 * qu'à ramener ce que personne ne regarde.
 */
@Entity
@Table(name = "game_list", indexes = @Index(name = "idx_game_list_user", columnList = "user_id"))
public class GameListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListVisibility visibility = ListVisibility.PRIVATE;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ListVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(ListVisibility visibility) {
        this.visibility = visibility;
    }

    public boolean isPublic() {
        return visibility == ListVisibility.PUBLIC;
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
