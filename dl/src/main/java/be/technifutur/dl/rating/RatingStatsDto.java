package be.technifutur.dl.rating;

import java.util.List;

public class RatingStatsDto {

    private int totalRated;
    private Double averageRating;
    private String topGenre;
    private int topGenreCount;
    private GameRatingDto bestRatedGame;
    private List<GenreCountDto> genreBreakdown;
    private List<RatingBucketDto> ratingDistribution;
    private TasteComparisonDto tasteComparison;

    public int getTotalRated() {
        return totalRated;
    }

    public void setTotalRated(int totalRated) {
        this.totalRated = totalRated;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public String getTopGenre() {
        return topGenre;
    }

    public void setTopGenre(String topGenre) {
        this.topGenre = topGenre;
    }

    public int getTopGenreCount() {
        return topGenreCount;
    }

    public void setTopGenreCount(int topGenreCount) {
        this.topGenreCount = topGenreCount;
    }

    public GameRatingDto getBestRatedGame() {
        return bestRatedGame;
    }

    public void setBestRatedGame(GameRatingDto bestRatedGame) {
        this.bestRatedGame = bestRatedGame;
    }

    public List<GenreCountDto> getGenreBreakdown() {
        return genreBreakdown;
    }

    public void setGenreBreakdown(List<GenreCountDto> genreBreakdown) {
        this.genreBreakdown = genreBreakdown;
    }

    public List<RatingBucketDto> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(List<RatingBucketDto> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    public TasteComparisonDto getTasteComparison() {
        return tasteComparison;
    }

    public void setTasteComparison(TasteComparisonDto tasteComparison) {
        this.tasteComparison = tasteComparison;
    }
}
