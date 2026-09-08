package be.technifutur.dl.list;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Ajout d'un jeu à une liste.
 * <p>
 * Le titre et la jaquette accompagnent la demande : une liste peut contenir un jeu que
 * personne n'a noté, dont le site n'a donc aucune autre trace, et l'afficher ne doit pas
 * dépendre d'un appel à IGDB.
 */
public class AddListItemRequestDto {

    @NotNull
    private Long igdbGameId;

    @Size(max = 255)
    private String title;

    @Size(max = 2048)
    private String coverUrl;

    private Long releaseDate;

    @Size(max = 280)
    private String note;

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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
