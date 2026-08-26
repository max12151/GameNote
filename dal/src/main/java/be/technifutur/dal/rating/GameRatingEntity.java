package be.technifutur.dal.rating;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.List;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

@Entity
@Table(name = "game_rating", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "igdb_game_id"}))
public class GameRatingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "igdb_game_id", nullable = false)
    private Long igdbGameId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "release_date")
    private Long releaseDate;

    // @Fetch(SUBSELECT) sur les 4 collections : quand on charge une liste de notes (ex. la
    // collection d'un utilisateur), Hibernate ne fait plus une requête par ligne et par
    // collection (N+1, jusqu'à 4×N requêtes) mais une seule requête par collection pour
    // l'ensemble du lot, via une sous-requête réutilisant le WHERE de la requête principale.
    @ElementCollection
    @CollectionTable(name = "game_rating_genre", joinColumns = @JoinColumn(name = "game_rating_id"))
    @Column(name = "genre", length = 100)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> genres;

    @Column(columnDefinition = "text")
    private String summary;

    @ElementCollection
    @CollectionTable(name = "game_rating_developer", joinColumns = @JoinColumn(name = "game_rating_id"))
    @Column(name = "developer", length = 255)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> developers;

    @ElementCollection
    @CollectionTable(name = "game_rating_publisher", joinColumns = @JoinColumn(name = "game_rating_id"))
    @Column(name = "publisher", length = 255)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> publishers;

    @ElementCollection
    @CollectionTable(name = "game_rating_platform", joinColumns = @JoinColumn(name = "game_rating_id"))
    @Column(name = "platform", length = 100)
    @Fetch(FetchMode.SUBSELECT)
    private List<String> platforms;

    @Column(nullable = false)
    private Integer rating;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public Long getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Long releaseDate) {
        this.releaseDate = releaseDate;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getDevelopers() {
        return developers;
    }

    public void setDevelopers(List<String> developers) {
        this.developers = developers;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public void setPublishers(List<String> publishers) {
        this.publishers = publishers;
    }

    public List<String> getPlatforms() {
        return platforms;
    }

    public void setPlatforms(List<String> platforms) {
        this.platforms = platforms;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
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
