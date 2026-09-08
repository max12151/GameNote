package be.technifutur.bll.list;

import be.technifutur.dal.list.GameListItemEntity;
import be.technifutur.dal.list.GameListSummaryView;
import java.util.List;

/** Une liste et ses jeux, dans l'ordre voulu par son auteur. */
public record GameListDetail(GameListSummaryView summary, List<GameListItemEntity> items) {
}
