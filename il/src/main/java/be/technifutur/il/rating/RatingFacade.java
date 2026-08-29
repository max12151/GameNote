package be.technifutur.il.rating;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingStatsDto;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RatingFacade {

    private final UserService userService;
    private final GameRatingService gameRatingService;
    private final GameCommentService gameCommentService;
    private final GameRatingMapper gameRatingMapper;

    public RatingFacade(UserService userService,
                        GameRatingService gameRatingService,
                        GameCommentService gameCommentService,
                        GameRatingMapper gameRatingMapper) {
        this.userService = userService;
        this.gameRatingService = gameRatingService;
        this.gameCommentService = gameCommentService;
        this.gameRatingMapper = gameRatingMapper;
    }

    public GameRatingDto rateGame(String username, RateGameRequestDto request) {
        UserEntity user = userService.getByUsername(username);
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
        UserEntity user = userService.getByUsername(username);
        return gameRatingService.getRatingsForUser(user.getId()).stream()
                .map(gameRatingMapper::toDto)
                .toList();
    }

    /**
     * Retirer un jeu de sa collection retire aussi l'avis qu'on avait laissé dessus : le
     * commentaire n'a de sens qu'accompagné de la note qui le justifie. Les deux suppressions
     * partagent une transaction pour ne jamais laisser un commentaire orphelin.
     */
    @Transactional
    public void removeRating(String username, Long igdbGameId) {
        UserEntity user = userService.getByUsername(username);
        gameCommentService.deleteOwnComment(user.getId(), igdbGameId);
        gameRatingService.removeRating(user.getId(), igdbGameId);
    }

    public RatingStatsDto getStats(String username) {
        UserEntity user = userService.getByUsername(username);
        return gameRatingMapper.toStatsDto(gameRatingService.getStats(user.getId()));
    }
}
