package be.technifutur.dl.list;

import java.util.List;

/** Une liste et ses jeux, dans l'ordre voulu par son auteur. */
public record GameListDetailDto(GameListDto list, List<GameListItemDto> items) {
}
