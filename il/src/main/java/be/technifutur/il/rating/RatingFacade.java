package be.technifutur.il.rating;

import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingStatsDto;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RatingFacade {

    private final UserService userService;
    private final GameRatingService gameRatingService;
    private final GameRatingMapper gameRatingMapper;

    public RatingFacade(UserService userService,
                        GameRatingService gameRatingService,
                        GameRatingMapper gameRatingMapper) {
        this.userService = userService;
        this.gameRatingService = gameRatingService;
        this.gameRatingMapper = gameRatingMapper;
    }

    public GameRatingDto rateGame(String username, RateGameRequestDto request) {
        UserEntity user = resolveUser(username);
        return gameRatingMapper.toDto(gameRatingService.rateGame(
                user.getId(),
                request.getIgdbGameId(),
                request.getTitle(),
                request.getCoverUrl(),
                request.getReleaseDate(),
                request.getGenres(),
                request.getSummary(),
                request.getDevelopers(),
                request.getPublishers(),
                request.getPlatforms(),
                request.getRating()
        ));
    }

    public List<GameRatingDto> getCollection(String username) {
        UserEntity user = resolveUser(username);
        return gameRatingService.getRatingsForUser(user.getId()).stream()
                .map(gameRatingMapper::toDto)
                .toList();
    }

    public void removeRating(String username, Long igdbGameId) {
        UserEntity user = resolveUser(username);
        gameRatingService.removeRating(user.getId(), igdbGameId);
    }

    public RatingStatsDto getStats(String username) {
        UserEntity user = resolveUser(username);
        return gameRatingMapper.toStatsDto(gameRatingService.getStats(user.getId()));
    }

    public Set<Long> getRatedIgdbGameIds(String username) {
        UserEntity user = resolveUser(username);
        return Set.copyOf(gameRatingService.getRatedIgdbGameIds(user.getId()));
    }

    private UserEntity resolveUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
    }
}
