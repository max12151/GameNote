package be.technifutur.dal.list;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Un jeu dans une liste, à la place que son auteur lui a donnée.
 * <p>
 * Titre et jaquette sont recopiés depuis IGDB plutôt que retrouvés via les notes du site :
 * une liste peut contenir un jeu que personne n'a noté, dont la base n'a donc aucune autre
 * trace, et l'afficher ne doit pas dépendre d'un appel réseau.
 * <p>
 * La colonne d'ordre s'appelle {@code sort_index} et non {@code position}, qui est une
 * fonction SQL et obligerait à des guillemets partout.
 */
@Entity
@Table(
        name = "game_list_item",
        uniqueConstraints = @UniqueConstraint(columnNames = {"game_list_id", "igdb_game_id"}),
        indexes = @Index(name = "idx_game_list_item_order", columnList = "game_list_id, sort_index")
)
public class GameListItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_list_id", nullable = false)
    private Long gameListId;

    @Column(name = "igdb_game_id", nullable = false)
    private Long igdbGameId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "release_date")
    private Long releaseDate;

    /** Mot de l'auteur sur ce jeu-là, propre à cette liste. */
    @Column(length = 280)
    private String note;

    /** Rang dans la liste, à partir de 0. Contigu après chaque ajout, retrait ou déplacement. */
    @Column(name = "sort_index", nullable = false)
    private Integer sortIndex;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGameListId() {
        return gameListId;
    }

    public void setGameListId(Long gameListId) {
        this.gameListId = gameListId;
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

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getSortIndex() {
        return sortIndex;
    }

    public void setSortIndex(Integer sortIndex) {
        this.sortIndex = sortIndex;
    }

    public OffsetDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(OffsetDateTime addedAt) {
        this.addedAt = addedAt;
    }
}
