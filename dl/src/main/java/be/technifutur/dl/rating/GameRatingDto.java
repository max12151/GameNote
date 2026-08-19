package be.technifutur.dl.rating;

import java.time.OffsetDateTime;
import java.util.List;

public class GameRatingDto {

    private Long id;
    private Long igdbGameId;
    private String title;
    private String coverUrl;
    private Long releaseDate;
    private List<String> genres;
    private Integer rating;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
