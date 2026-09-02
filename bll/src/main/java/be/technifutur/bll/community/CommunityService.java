package be.technifutur.bll.community;

import be.technifutur.dal.rating.CommunityRankingView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.GameVoteView;
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

        RankingWeights weights = computeWeights();

        // PageRequest volontairement non trié : le tri porte sur des agrégats et est déjà
        // écrit dans la requête du repository (cf. findCommunityRanking).
        List<CommunityRankingView> games = gameRatingRepository.findCommunityRanking(
                safeSearch,
                weights.globalAverage(),
                weights.minimumVotes(),
                PageRequest.of(safePage, safeSize)
        );

        return new CommunityRanking(
                games,
                gameRatingRepository.countRatedGames(safeSearch),
                safePage,
                safeSize,
                weights
        );
    }

    /**
     * Établit les constantes de la pondération à partir de l'état réel du site.
     * <p>
     * Une requête agrégée ramène un couple (votes, moyenne) par jeu — quelques centaines de
     * lignes de deux nombres, pas les notes elles-mêmes. La médiane se calcule ensuite en
     * mémoire, faute d'exister en JPQL, et parce que la trier en base pour une valeur
     * unique n'apporterait rien à cette échelle.
     */
    private RankingWeights computeWeights() {
        List<GameVoteView> votes = gameRatingRepository.findVotesPerGame();

        if (votes.isEmpty()) {
            return new RankingWeights(0, 1);
        }

        double globalAverage = votes.stream()
                .mapToDouble(GameVoteView::getAverageRating)
                .average()
                .orElse(0);

        long[] counts = votes.stream()
                .mapToLong(GameVoteView::getRatingCount)
                .sorted()
                .toArray();

        // Un seuil nul annulerait la pondération : la formule rendrait la moyenne brute.
        return new RankingWeights(globalAverage, Math.max(median(counts), 1));
    }

    /** Médiane d'un tableau déjà trié ; moyenne des deux valeurs centrales si la taille est paire. */
    private static double median(long[] sorted) {
        int middle = sorted.length / 2;

        return sorted.length % 2 == 1
                ? sorted[middle]
                : (sorted[middle - 1] + sorted[middle]) / 2.0;
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
