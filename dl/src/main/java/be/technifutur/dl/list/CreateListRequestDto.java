package be.technifutur.dl.list;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Création ou modification d'une liste : les deux prennent exactement les mêmes champs. */
public class CreateListRequestDto {

    @NotBlank
    @Size(max = 100)
    private String name;

    @Size(max = 500)
    private String description;

    /**
     * Absente, la liste est créée privée. Une liste publiée par défaut exposerait un brouillon
     * que son auteur croyait à lui.
     */
    private ListVisibilityDto visibility;

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

    public ListVisibilityDto getVisibility() {
        return visibility;
    }

    public void setVisibility(ListVisibilityDto visibility) {
        this.visibility = visibility;
    }
}
