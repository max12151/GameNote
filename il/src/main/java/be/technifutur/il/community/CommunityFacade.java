package be.technifutur.il.community;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.community.CommunityGameDetail;
import be.technifutur.bll.community.CommunityRanking;
import be.technifutur.bll.community.CommunityService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.rating.CommunityGameView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.il.comment.GameCommentMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CommunityFacade {

    private final UserService userService;
    private final CommunityService communityService;
    private final GameCommentService gameCommentService;
    private final GameRatingService gameRatingService;
    private final CommunityMapper communityMapper;
    private final GameCommentMapper gameCommentMapper;

    public CommunityFacade(UserService userService,
                           CommunityService communityService,
                           GameCommentService gameCommentService,
                           GameRatingService gameRatingService,
                           CommunityMapper communityMapper,
                           GameCommentMapper gameCommentMapper) {
        this.userService = userService;
        this.communityService = communityService;
        this.gameCommentService = gameCommentService;
        this.gameRatingService = gameRatingService;
        this.communityMapper = communityMapper;
        this.gameCommentMapper = gameCommentMapper;
    }

    public CommunityRankingDto getRanking(int page, int size) {
        CommunityRanking ranking = communityService.getRanking(page, size);

        // Une seule requête de comptage pour toute la page, au lieu d'une par jeu affiché.
        List<Long> igdbGameIds = ranking.games().stream()
                .map(CommunityGameView::getIgdbGameId)
                .toList();

        return communityMapper.toRankingDto(ranking, gameCommentService.countCommentsByGame(igdbGameIds));
    }

    public CommunityGameDetailDto getGameDetail(String username, Long igdbGameId) {
        UserEntity user = userService.getByUsername(username);

        CommunityGameDetail detail = communityService.getGameDetail(igdbGameId);

        List<GameCommentDto> comments = gameCommentService.getComments(igdbGameId).stream()
                .map(view -> gameCommentMapper.toDto(view, user.getId(), user.isAdmin()))
                .toList();

        Integer myRating = gameRatingService.findRating(user.getId(), igdbGameId)
                .map(GameRatingEntity::getRating)
                .orElse(null);

        // Le commentaire de l'utilisateur courant se retrouve dans la liste déjà chargée :
        // pas la peine d'aller le rechercher en base une seconde fois.
        GameCommentDto myComment = comments.stream()
                .filter(GameCommentDto::mine)
                .findFirst()
                .orElse(null);

        return communityMapper.toDetailDto(detail, comments, myRating, myComment);
    }
}
