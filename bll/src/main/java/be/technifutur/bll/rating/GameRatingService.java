package be.technifutur.bll.rating;

import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameRatingRepository;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GameRatingService {

    private static final int GENRE_BREAKDOWN_SIZE = 5;

    private final GameRatingRepository gameRatingRepository;

    public GameRatingService(GameRatingRepository gameRatingRepository) {
        this.gameRatingRepository = gameRatingRepository;
    }

    @Transactional
    public GameRatingEntity rateGame(Long userId,
                                     Long igdbGameId,
                                     String title,
                                     String coverUrl,
                                     Long releaseDate,
                                     List<String> genres,
                                     String summary,
                                     List<String> developers,
                                     List<String> publishers,
                                     List<String> platforms,
                                     int rating) {
        GameRatingEntity entity = gameRatingRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseGet(() -> {
                    GameRatingEntity created = new GameRatingEntity();
                    created.setUserId(userId);
                    created.setIgdbGameId(igdbGameId);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });

        entity.setTitle(title);
        entity.setCoverUrl(coverUrl);
        entity.setReleaseDate(releaseDate);
        entity.setGenres(genres);
        entity.setSummary(summary);
        entity.setDevelopers(developers);
        entity.setPublishers(publishers);
        entity.setPlatforms(platforms);
        entity.setRating(rating);

        return gameRatingRepository.save(entity);
    }

    public List<GameRatingEntity> getRatingsForUser(Long userId) {
        return gameRatingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Long> getRatedIgdbGameIds(Long userId) {
        return gameRatingRepository.findIgdbGameIdsByUserId(userId);
    }

    @Transactional
    public void removeRating(Long userId, Long igdbGameId) {
        GameRatingEntity entity = gameRatingRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseThrow(() -> new ResourceNotFoundException("Note introuvable"));

        gameRatingRepository.delete(entity);
    }

    public RatingStats getStats(Long userId) {
        List<GameRatingEntity> ratings = getRatingsForUser(userId);

        if (ratings.isEmpty()) {
            return new RatingStats(0, null, null, 0, null, List.of());
        }

        double average = ratings.stream()
                .mapToInt(GameRatingEntity::getRating)
                .average()
                .orElse(0);

        List<GenreCount> genreBreakdown = ratings.stream()
                .flatMap(r -> r.getGenres() == null ? List.<String>of().stream() : r.getGenres().stream())
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new GenreCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(GenreCount::count).reversed())
                .limit(GENRE_BREAKDOWN_SIZE)
                .toList();

        GenreCount topGenre = genreBreakdown.isEmpty() ? null : genreBreakdown.get(0);

        GameRatingEntity bestRatedGame = ratings.stream()
                .max(Comparator.<GameRatingEntity>comparingInt(GameRatingEntity::getRating)
                        .thenComparing(GameRatingEntity::getCreatedAt))
                .orElse(null);

        return new RatingStats(
                ratings.size(),
                average,
                topGenre != null ? topGenre.genre() : null,
                topGenre != null ? (int) topGenre.count() : 0,
                bestRatedGame,
                genreBreakdown
        );
    }
}
