package be.technifutur.dl.list;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Déplacement d'un jeu dans une liste.
 *
 * @see #getPosition() rang voulu, à partir de 1 comme à l'écran
 */
public class MoveListItemRequestDto {

    @NotNull
    @Min(1)
    private Integer position;

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
