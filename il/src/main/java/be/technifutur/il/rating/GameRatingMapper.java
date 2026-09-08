package be.technifutur.il.rating;

import be.technifutur.bll.rating.GameMetadata;
import be.technifutur.bll.rating.GenreCount;
import be.technifutur.bll.rating.LibrarySummary;
import be.technifutur.bll.rating.RatingStats;
import be.technifutur.bll.rating.TasteComparison;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameStatus;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GameStatusDto;
import be.technifutur.dl.rating.GenreCountDto;
import be.technifutur.dl.rating.LibrarySummaryDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingBucketDto;
import be.technifutur.dl.rating.RatingStatsDto;
import be.technifutur.dl.rating.SetStatusRequestDto;
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
        dto.setStatus(toStatusDto(entity.getStatus()));
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

    public LibrarySummaryDto toLibrarySummaryDto(LibrarySummary summary) {
        return new LibrarySummaryDto(
                summary.wishlist(),
                summary.playing(),
                summary.finished(),
                summary.total()
        );
    }

    /**
     * Les métadonnées du jeu telles qu'elles arrivent d'une demande de notation.
     * <p>
     * Le titre a une valeur de repli : le contrat exige un titre pour noter, mais la même
     * conversion sert aussi au rangement par statut, où le client peut n'envoyer que
     * l'identifiant d'un jeu qu'il vient de voir passer.
     */
    public GameMetadata toMetadata(RateGameRequestDto request) {
        return new GameMetadata(
                request.getIgdbGameId(),
                request.getTitle(),
                request.getCoverUrl(),
                request.getReleaseDate(),
                request.getSummary(),
                request.getGenres(),
                request.getDevelopers(),
                request.getPublishers(),
                request.getPlatforms()
        );
    }

    public GameMetadata toMetadata(SetStatusRequestDto request) {
        return new GameMetadata(
                request.getIgdbGameId(),
                request.getTitle() == null || request.getTitle().isBlank()
                        ? "Jeu #" + request.getIgdbGameId()
                        : request.getTitle(),
                request.getCoverUrl(),
                request.getReleaseDate(),
                request.getSummary(),
                request.getGenres(),
                request.getDevelopers(),
                request.getPublishers(),
                request.getPlatforms()
        );
    }

    /**
     * Les deux énumérations portent les mêmes noms, mais restent deux types : le contrat de
     * l'API ne doit pas changer parce qu'une constante interne a été renommée. La conversion
     * par nom rend cette correspondance explicite, et la casse au premier écart.
     */
    public GameStatusDto toStatusDto(GameStatus status) {
        return status == null ? null : GameStatusDto.valueOf(status.name());
    }

    public GameStatus toStatus(GameStatusDto status) {
        return status == null ? null : GameStatus.valueOf(status.name());
    }
}
