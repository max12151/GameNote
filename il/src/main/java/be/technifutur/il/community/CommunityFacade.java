package be.technifutur.il.community;

import be.technifutur.bll.comment.CommentSort;
import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.community.CommunityGameDetail;
import be.technifutur.bll.community.CommunityRanking;
import be.technifutur.bll.community.CommunityService;
import be.technifutur.bll.community.RankingQuery;
import be.technifutur.bll.list.GameListService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.reaction.CommentReactionService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.comment.GameCommentView;
import be.technifutur.dal.rating.CommunityGameView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.dl.community.RankingFacetsDto;
import be.technifutur.il.comment.GameCommentMapper;
import be.technifutur.il.rating.GameRatingMapper;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class CommunityFacade {

    private final UserService userService;
    private final CommunityService communityService;
    private final GameCommentService gameCommentService;
    private final GameRatingService gameRatingService;
    private final CommentReactionService commentReactionService;
    private final GameListService gameListService;
    private final CommunityMapper communityMapper;
    private final GameCommentMapper gameCommentMapper;
    private final GameRatingMapper gameRatingMapper;

    public CommunityFacade(UserService userService,
                           CommunityService communityService,
                           GameCommentService gameCommentService,
                           GameRatingService gameRatingService,
                           CommentReactionService commentReactionService,
                           GameListService gameListService,
                           CommunityMapper communityMapper,
                           GameCommentMapper gameCommentMapper,
                           GameRatingMapper gameRatingMapper) {
        this.userService = userService;
        this.communityService = communityService;
        this.gameCommentService = gameCommentService;
        this.gameRatingService = gameRatingService;
        this.commentReactionService = commentReactionService;
        this.gameListService = gameListService;
        this.communityMapper = communityMapper;
        this.gameCommentMapper = gameCommentMapper;
        this.gameRatingMapper = gameRatingMapper;
    }

    public CommunityRankingDto getRanking(int page, int size, RankingQuery query) {
        CommunityRanking ranking = communityService.getRanking(page, size, query);

        // Une seule requête de comptage pour toute la page, au lieu d'une par jeu affiché.
        List<Long> igdbGameIds = ranking.games().stream()
                .map(CommunityGameView::getIgdbGameId)
                .toList();

        return communityMapper.toRankingDto(ranking, gameCommentService.countCommentsByGame(igdbGameIds));
    }

    /** Les valeurs proposées dans les filtres, mises en cache côté service. */
    public RankingFacetsDto getFacets() {
        return communityMapper.toFacetsDto(communityService.getFacets());
    }

    /**
     * Renvoie {@link Optional#empty()} quand personne n'a encore noté le jeu : c'est à
     * l'appelant de décider quoi présenter, l'API IGDB restant la seule à connaître un jeu
     * dont le site n'a aucune trace.
     */
    public Optional<CommunityGameDetailDto> findGameDetail(String username, Long igdbGameId, CommentSort sort) {
        UserEntity user = userService.getByUsername(username);

        CommunityGameDetail detail = communityService.findGameDetail(igdbGameId).orElse(null);

        if (detail == null) {
            return Optional.empty();
        }

        List<GameCommentView> views = gameCommentService.getComments(igdbGameId, sort);

        // Les avis déjà marqués comme utiles par ce lecteur, en une requête pour tout le fil.
        Set<Long> reacted = commentReactionService.getReactedCommentIds(user.getId(),
                views.stream().map(GameCommentView::getId).toList());

        List<GameCommentDto> comments = views.stream()
                .map(view -> gameCommentMapper.toDto(view, user.getId(), user.isAdmin(), reacted))
                .toList();

        GameRatingEntity myEntry = gameRatingService.findRating(user.getId(), igdbGameId).orElse(null);

        // Le commentaire de l'utilisateur courant se retrouve dans la liste déjà chargée :
        // pas la peine d'aller le rechercher en base une seconde fois.
        GameCommentDto myComment = comments.stream()
                .filter(GameCommentDto::mine)
                .findFirst()
                .orElse(null);

        return Optional.of(communityMapper.toDetailDto(
                detail,
                comments,
                myEntry == null ? null : myEntry.getRating(),
                myComment,
                myEntry == null ? null : gameRatingMapper.toStatusDto(myEntry.getStatus()),
                List.copyOf(gameListService.getListIdsContaining(user.getId(), igdbGameId)),
                (sort == null ? CommentSort.RECENT : sort).name()));
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
