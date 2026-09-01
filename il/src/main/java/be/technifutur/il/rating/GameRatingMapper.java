package be.technifutur.il.rating;

import be.technifutur.bll.rating.GenreCount;
import be.technifutur.bll.rating.RatingStats;
import be.technifutur.bll.rating.TasteComparison;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GenreCountDto;
import be.technifutur.dl.rating.RatingBucketDto;
import be.technifutur.dl.rating.RatingStatsDto;
import be.technifutur.dl.rating.TasteComparisonDto;
import org.springframework.stereotype.Component;

@Component
public class GameRatingMapper {

    public GameRatingDto toDto(GameRatingEntity entity) {
        if (entity == null) {
            return null;
        }

        GameRatingDto dto = new GameRatingDto();
        dto.setId(entity.getId());
        dto.setIgdbGameId(entity.getIgdbGameId());
        dto.setTitle(entity.getTitle());
        dto.setCoverUrl(entity.getCoverUrl());
        dto.setReleaseDate(entity.getReleaseDate());
        dto.setGenres(entity.getGenres());
        dto.setSummary(entity.getSummary());
        dto.setDevelopers(entity.getDevelopers());
        dto.setPublishers(entity.getPublishers());
        dto.setPlatforms(entity.getPlatforms());
        dto.setRating(entity.getRating());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    public RatingStatsDto toStatsDto(RatingStats stats) {
        RatingStatsDto dto = new RatingStatsDto();
        dto.setTotalRated(stats.totalRated());
        dto.setAverageRating(stats.averageRating());
        dto.setTopGenre(stats.topGenre());
        dto.setTopGenreCount(stats.topGenreCount());
        dto.setBestRatedGame(toDto(stats.bestRatedGame()));
        dto.setGenreBreakdown(stats.genreBreakdown().stream()
                .map(this::toGenreCountDto)
                .toList());
        dto.setRatingDistribution(stats.ratingDistribution().stream()
                .map(bucket -> new RatingBucketDto(bucket.rating(), bucket.count()))
                .toList());
        dto.setTasteComparison(toTasteComparisonDto(stats.tasteComparison()));
        return dto;
    }

    private TasteComparisonDto toTasteComparisonDto(TasteComparison comparison) {
        return new TasteComparisonDto(
                comparison.averageDelta(),
                comparison.comparedGames(),
                comparison.stricter(),
                comparison.aligned(),
                comparison.generous()
        );
    }

    /** Publique : le profil public d'un joueur affiche la même répartition par genre. */
    public GenreCountDto toGenreCountDto(GenreCount genreCount) {
        return new GenreCountDto(genreCount.genre(), genreCount.count());
    }
}
