package be.technifutur.bll.rating;

import be.technifutur.dal.rating.GameRatingEntity;
import java.util.List;

public record RatingStats(
        int totalRated,
        Double averageRating,
        String topGenre,
        int topGenreCount,
        GameRatingEntity bestRatedGame,
        List<GenreCount> genreBreakdown,
        List<RatingBucket> ratingDistribution,
        TasteComparison tasteComparison
) {
}
