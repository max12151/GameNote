package be.technifutur.il.list;

import be.technifutur.dal.list.GameListItemEntity;
import be.technifutur.dal.list.GameListSummaryView;
import be.technifutur.dal.list.ListVisibility;
import be.technifutur.dl.list.GameListDto;
import be.technifutur.dl.list.GameListItemDto;
import be.technifutur.dl.list.ListVisibilityDto;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GameListMapper {

    /**
     * Une liste dans un index.
     *
     * @param coverPreview jaquettes ramenées à part : elles viennent d'une requête limitée à
     *                     quelques lignes, et non du contenu entier de la liste
     * @param viewerId     identifiant de qui regarde, pour le drapeau {@code mine} dont
     *                     dépendent les boutons de modification
     */
    public GameListDto toDto(GameListSummaryView view, List<String> coverPreview, Long viewerId) {
        return new GameListDto(
                view.getId(),
                view.getName(),
                view.getDescription(),
                toVisibilityDto(view.getVisibility()),
                view.getItemCount(),
                coverPreview,
                view.getOwnerId(),
                view.getOwnerUsername(),
                view.getOwnerId().equals(viewerId),
                view.getCreatedAt(),
                view.getUpdatedAt()
        );
    }

    /**
     * Un jeu de la liste. Le rang stocké part de zéro, celui qu'on affiche part de un : la
     * conversion se fait ici pour que le front n'ait pas à recompter, et que le corps de la
     * demande de déplacement parle le même langage que l'écran.
     */
    public GameListItemDto toItemDto(GameListItemEntity entity, int index) {
        return new GameListItemDto(
                entity.getIgdbGameId(),
                entity.getTitle(),
                entity.getCoverUrl(),
                entity.getReleaseDate(),
                entity.getNote(),
                index + 1,
                entity.getAddedAt()
        );
    }

    public ListVisibilityDto toVisibilityDto(ListVisibility visibility) {
        return visibility == null ? null : ListVisibilityDto.valueOf(visibility.name());
    }

    public ListVisibility toVisibility(ListVisibilityDto visibility) {
        return visibility == null ? null : ListVisibility.valueOf(visibility.name());
    }
}
