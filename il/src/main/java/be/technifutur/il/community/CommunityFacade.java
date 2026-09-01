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
import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.il.comment.GameCommentMapper;
import java.util.List;
import java.util.Optional;
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

    public CommunityRankingDto getRanking(int page, int size, String search) {
        CommunityRanking ranking = communityService.getRanking(page, size, search);

        // Une seule requête de comptage pour toute la page, au lieu d'une par jeu affiché.
        List<Long> igdbGameIds = ranking.games().stream()
                .map(CommunityGameView::getIgdbGameId)
                .toList();

        return communityMapper.toRankingDto(ranking, gameCommentService.countCommentsByGame(igdbGameIds));
    }

    /**
     * Renvoie {@link Optional#empty()} quand personne n'a encore noté le jeu : c'est à
     * l'appelant de décider quoi présenter, l'API IGDB restant la seule à connaître un jeu
     * dont le site n'a aucune trace.
     */
    public Optional<CommunityGameDetailDto> findGameDetail(String username, Long igdbGameId) {
        UserEntity user = userService.getByUsername(username);

        CommunityGameDetail detail = communityService.findGameDetail(igdbGameId).orElse(null);

        if (detail == null) {
            return Optional.empty();
        }

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

        return Optional.of(communityMapper.toDetailDto(detail, comments, myRating, myComment));
    }

    /**
     * Derniers avis publiés sur le site, tous jeux confondus.
     * <p>
     * Aucun utilisateur courant n'entre en jeu : ce flux ne sert qu'à donner envie
     * d'ouvrir une fiche, et n'expose donc aucun droit de modération. C'est la fiche du
     * jeu, elle, qui sait qui peut supprimer quoi.
     */
    public List<RecentCommentDto> getRecentComments(int limit) {
        return gameCommentService.getRecentComments(limit).stream()
                .map(gameCommentMapper::toRecentDto)
                .toList();
    }
}
