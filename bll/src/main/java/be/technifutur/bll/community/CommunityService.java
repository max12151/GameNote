package be.technifutur.bll.community;

import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.rating.CommunityGameView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.RatingBucketView;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class CommunityService {

    private static final int MAX_PAGE_SIZE = 50;

    private final GameRatingRepository gameRatingRepository;

    public CommunityService(GameRatingRepository gameRatingRepository) {
        this.gameRatingRepository = gameRatingRepository;
    }

    /**
     * Classement des jeux notés sur le site, du mieux noté au moins bien noté.
     * L'agrégation est faite par la base : seule la page demandée est chargée.
     */
    public CommunityRanking getRanking(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        // PageRequest volontairement non trié : le tri porte sur des agrégats et est déjà
        // écrit dans la requête du repository (cf. findCommunityRanking).
        List<CommunityGameView> games =
                gameRatingRepository.findCommunityRanking(PageRequest.of(safePage, safeSize));

        return new CommunityRanking(games, gameRatingRepository.countRatedGames(), safePage, safeSize);
    }

    public CommunityGameDetail getGameDetail(Long igdbGameId) {
        GameRatingEntity game = gameRatingRepository
                .findDescriptiveRatings(igdbGameId, PageRequest.of(0, 1)).stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Aucun avis pour ce jeu"));

        List<RatingBucketView> distribution = gameRatingRepository.findRatingDistribution(igdbGameId);

        // Moyenne et total se déduisent de l'histogramme : inutile de redemander un AVG/COUNT
        // à la base, qui relirait exactement les mêmes lignes.
        long ratingCount = 0;
        long weightedSum = 0;

        for (RatingBucketView bucket : distribution) {
            ratingCount += bucket.getCount();
            weightedSum += (long) bucket.getRating() * bucket.getCount();
        }

        double averageRating = ratingCount == 0 ? 0 : (double) weightedSum / ratingCount;

        return new CommunityGameDetail(game, averageRating, ratingCount, distribution);
    }
}
