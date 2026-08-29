package be.technifutur.bll.community;

import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.RatingBucketView;
import java.util.List;

/**
 * Fiche communautaire d'un jeu : ses métadonnées (reprises d'une note existante), la
 * moyenne des joueurs du site, le nombre de votes et la répartition des notes.
 */
public record CommunityGameDetail(GameRatingEntity game,
                                  double averageRating,
                                  long ratingCount,
                                  List<RatingBucketView> distribution) {
}
