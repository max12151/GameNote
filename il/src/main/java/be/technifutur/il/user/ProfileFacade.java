package be.technifutur.il.user;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.follow.FollowService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.AvatarImage;
import be.technifutur.bll.user.AvatarService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserSearchView;
import be.technifutur.dl.comment.CommentPageDto;
import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.user.PublicProfileDto;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.dl.user.UserSearchResultDto;
import be.technifutur.il.comment.GameCommentMapper;
import be.technifutur.il.follow.FollowFacade;
import be.technifutur.il.list.GameListFacade;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ProfileFacade {

    /**
     * Taille d'une page d'avis sur un profil. Assez pour donner le ton d'un joueur sans
     * transformer sa fiche en fil sans fin ; les suivantes se chargent à la demande.
     */
    private static final int PROFILE_COMMENTS = 10;

    private final UserService userService;
    private final AvatarService avatarService;
    private final GameRatingService gameRatingService;
    private final GameCommentService gameCommentService;
    private final FollowService followService;
    private final FollowFacade followFacade;
    private final GameListFacade gameListFacade;
    private final UserMapper userMapper;
    private final GameCommentMapper gameCommentMapper;

    public ProfileFacade(UserService userService,
                         AvatarService avatarService,
                         GameRatingService gameRatingService,
                         GameCommentService gameCommentService,
                         FollowService followService,
                         FollowFacade followFacade,
                         GameListFacade gameListFacade,
                         UserMapper userMapper,
                         GameCommentMapper gameCommentMapper) {
        this.userService = userService;
        this.avatarService = avatarService;
        this.gameRatingService = gameRatingService;
        this.gameCommentService = gameCommentService;
        this.followService = followService;
        this.followFacade = followFacade;
        this.gameListFacade = gameListFacade;
        this.userMapper = userMapper;
        this.gameCommentMapper = gameCommentMapper;
    }

    public Optional<AvatarImage> findAvatar(Long userId) {
        return avatarService.findAvatar(userId);
    }

    public UserDto getProfile(String username) {
        return userMapper.toDto(userService.getByUsername(username));
    }

    public UserDto updateProfile(String username, UpdateProfileRequestDto request) {
        UserEntity user = userService.updateProfile(username, request.getBio(), request.getAvatarUrl());
        return userMapper.toDto(user);
    }

    /**
     * Profil d'un joueur consulté par un autre membre : ses statistiques de notation, sa
     * bibliothèque, ses listes publiques et ses derniers avis, sans rien de ce qui relève de
     * son compte.
     * <p>
     * {@link Optional#empty()} pour un identifiant inconnu, anonymisé <em>ou suspendu</em> :
     * le premier vient d'une URL que n'importe qui peut taper, les deux autres n'ont plus de
     * page à montrer. Aucun des trois n'est une anomalie.
     */
    public Optional<PublicProfileDto> getPublicProfile(String viewerUsername, Long userId) {
        UserEntity viewer = userService.getByUsername(viewerUsername);

        return userService.findVisibleById(userId).map(user -> userMapper.toPublicDto(
                user,
                gameRatingService.getStats(user.getId()),
                commentPage(user.getId(), 0, PROFILE_COMMENTS),
                gameCommentService.countCommentsByAuthor(user.getId()),
                followFacade.getStats(viewer.getId(), user.getId()),
                gameRatingService.getLibrarySummary(user.getId()),
                gameListFacade.getPublicListsOf(user.getId(), viewer.getId())
        ));
    }

    /**
     * Une page d'avis d'un joueur, pour la suite du fil déjà entamé par son profil.
     * <p>
     * {@link Optional#empty()} pour un identifiant inconnu, comme pour le profil lui-même :
     * il vient d'une URL que n'importe qui peut taper.
     */
    public Optional<CommentPageDto> getPlayerComments(Long userId, int page, int size) {
        if (userService.findVisibleById(userId).isEmpty()) {
            return Optional.empty();
        }

        int safePage = Math.max(page, 0);

        return Optional.of(new CommentPageDto(
                commentPage(userId, safePage, size),
                gameCommentService.countCommentsByAuthor(userId),
                safePage,
                size
        ));
    }

    /**
     * Le fil lui-même, sans contrôle d'existence : le profil vient de résoudre le joueur,
     * lui redemander à la base ne dirait rien de neuf.
     */
    private List<RecentCommentDto> commentPage(Long userId, int page, int size) {
        return gameCommentService.getCommentsByAuthor(userId, page, size).stream()
                .map(gameCommentMapper::toRecentDto)
                .toList();
    }

    /**
     * Membres dont le pseudo contient le terme cherché.
     * <p>
     * Le nombre de jeux notés et l'état du bouton « suivre » viennent chacun d'une seule
     * requête d'agrégat pour toute la liste, et non d'un appel par ligne affichée — le même
     * geste que le comptage des commentaires sur une page de classement.
     */
    public List<UserSearchResultDto> searchPlayers(String viewerUsername, String term, int limit) {
        List<UserSearchView> found = userService.search(term, limit);

        if (found.isEmpty()) {
            return List.of();
        }

        UserEntity viewer = userService.getByUsername(viewerUsername);
        List<Long> ids = found.stream().map(UserSearchView::getId).toList();

        Map<Long, Long> ratedGames = gameRatingService.countRatingsByUser(ids);
        Set<Long> followed = followService.getFollowedIdsAmong(viewer.getId(), ids);

        return found.stream()
                .map(view -> userMapper.toSearchDto(view,
                        ratedGames.getOrDefault(view.getId(), 0L),
                        followed.contains(view.getId())))
                .toList();
    }
}
