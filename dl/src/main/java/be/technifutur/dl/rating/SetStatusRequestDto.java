package be.technifutur.dl.rating;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Range un jeu dans la bibliothèque sous un statut donné, sans le noter.
 * <p>
 * Les métadonnées accompagnent la demande parce que le jeu peut n'avoir jamais été vu par le
 * site : ajouter un titre à sa liste d'envies depuis la recherche crée la première trace
 * qu'on en ait, et une fiche doit rester affichable même quand IGDB est injoignable.
 */
public class SetStatusRequestDto {

    @NotNull
    private Long igdbGameId;

    @NotNull
    private GameStatusDto status;

    @Size(max = 255)
    private String title;

    @Size(max = 2048)
    private String coverUrl;

    private Long releaseDate;

    private List<String> genres;

    @Size(max = 4000)
    private String summary;

    private List<String> developers;

    private List<String> publishers;

    private List<String> platforms;

    public Long getIgdbGameId() {
        return igdbGameId;
    }

    public void setIgdbGameId(Long igdbGameId) {
        this.igdbGameId = igdbGameId;
    }

    public GameStatusDto getStatus() {
        return status;
    }

    public void setStatus(GameStatusDto status) {
        this.status = status;
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
}
