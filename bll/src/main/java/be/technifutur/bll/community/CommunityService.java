package be.technifutur.bll.community;

import be.technifutur.dal.rating.CommunityGameView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.RatingBucketView;
import java.util.List;
import java.util.Optional;
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
     * Classement des jeux notés sur le site, du mieux noté au moins bien noté, filtré sur
     * le titre quand une recherche est fournie.
     * <p>
     * Le filtrage est fait par la base, et non sur la page déjà chargée : sans cela, un jeu
     * classé au-delà de la première page resterait introuvable tant qu'on n'aurait pas
     * déroulé le classement jusqu'à lui.
     */
    public CommunityRanking getRanking(int page, int size, String search) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSearch = search == null ? "" : search.strip();

        // PageRequest volontairement non trié : le tri porte sur des agrégats et est déjà
        // écrit dans la requête du repository (cf. findCommunityRanking).
        List<CommunityGameView> games =
                gameRatingRepository.findCommunityRanking(safeSearch, PageRequest.of(safePage, safeSize));

        return new CommunityRanking(
                games,
                gameRatingRepository.countRatedGames(safeSearch),
                safePage,
                safeSize
        );
    }

    /**
     * Fiche communautaire d'un jeu, ou {@link Optional#empty()} si personne ne l'a encore
     * noté. L'absence d'avis n'est pas une erreur : l'appelant peut alors présenter le jeu
     * en invitant à être le premier à le noter.
     */
    public Optional<CommunityGameDetail> findGameDetail(Long igdbGameId) {
        Optional<GameRatingEntity> game = gameRatingRepository
                .findDescriptiveRatings(igdbGameId, PageRequest.of(0, 1)).stream()
                .findFirst();

        if (game.isEmpty()) {
            return Optional.empty();
        }

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

        return Optional.of(new CommunityGameDetail(game.get(), averageRating, ratingCount, distribution));
    }
}
