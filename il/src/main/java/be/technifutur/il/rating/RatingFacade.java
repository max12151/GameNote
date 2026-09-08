package be.technifutur.il.rating;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.rating.GameStatus;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GameStatusDto;
import be.technifutur.dl.rating.LibrarySummaryDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingStatsDto;
import be.technifutur.dl.rating.SetStatusRequestDto;
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
                gameRatingMapper.toMetadata(request),
                request.getRating(),
                gameRatingMapper.toStatus(request.getStatus())
        ));
    }

    /**
     * Range un jeu dans la bibliothèque sans le noter, ou déplace celui qui s'y trouve déjà.
     * <p>
     * C'est le geste « ajouter à ma liste d'envies » depuis la recherche, et celui du
     * sélecteur de statut sur la fiche d'un jeu.
     */
    public GameRatingDto setStatus(String username, SetStatusRequestDto request) {
        UserEntity user = userService.getByUsername(username);

        return gameRatingMapper.toDto(gameRatingService.setStatus(
                user.getId(),
                gameRatingMapper.toMetadata(request),
                gameRatingMapper.toStatus(request.getStatus())
        ));
    }

    /**
     * La bibliothèque, entière ou restreinte à un statut.
     *
     * @param status onglet demandé, ou {@code null} pour tout voir
     */
    public List<GameRatingDto> getCollection(String username, GameStatusDto status) {
        UserEntity user = userService.getByUsername(username);
        GameStatus filter = gameRatingMapper.toStatus(status);

        return gameRatingService.getLibrary(user.getId(), filter).stream()
                .map(gameRatingMapper::toDto)
                .toList();
    }

    public LibrarySummaryDto getLibrarySummary(String username) {
        UserEntity user = userService.getByUsername(username);

        return gameRatingMapper.toLibrarySummaryDto(gameRatingService.getLibrarySummary(user.getId()));
    }

    /**
     * Retirer un jeu de sa bibliothèque retire aussi l'avis qu'on avait laissé dessus : le
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
