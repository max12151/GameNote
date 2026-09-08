package be.technifutur.il.follow;

import be.technifutur.bll.follow.FollowService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserSearchView;
import be.technifutur.dl.user.FollowStatsDto;
import be.technifutur.dl.user.PlayerListDto;
import be.technifutur.dl.user.UserSearchResultDto;
import be.technifutur.il.user.UserMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Le suivi entre joueurs, vu depuis l'API.
 * <p>
 * Chaque liste de membres servie ici passe par la même carte que les résultats de recherche :
 * un abonné, un abonnement et un résultat de recherche se présentent pareil, et doivent donc
 * porter les mêmes informations — dont le drapeau « je le suis déjà ».
 */
@Component
public class FollowFacade {

    private static final int DEFAULT_PROFILE_LIMIT = 50;

    private final UserService userService;
    private final FollowService followService;
    private final GameRatingService gameRatingService;
    private final UserMapper userMapper;

    public FollowFacade(UserService userService,
                        FollowService followService,
                        GameRatingService gameRatingService,
                        UserMapper userMapper) {
        this.userService = userService;
        this.followService = followService;
        this.gameRatingService = gameRatingService;
        this.userMapper = userMapper;
    }

    public FollowStatsDto follow(String username, Long targetUserId) {
        UserEntity me = userService.getByUsername(username);

        followService.follow(me.getId(), targetUserId);

        return getStats(me.getId(), targetUserId);
    }

    public FollowStatsDto unfollow(String username, Long targetUserId) {
        UserEntity me = userService.getByUsername(username);

        followService.unfollow(me.getId(), targetUserId);

        return getStats(me.getId(), targetUserId);
    }

    /** Compteurs d'un profil et état du bouton, du point de vue du membre qui regarde. */
    public FollowStatsDto getStats(Long viewerId, Long profileId) {
        boolean isMe = profileId.equals(viewerId);

        return new FollowStatsDto(
                followService.countFollowing(profileId),
                followService.countFollowers(profileId),
                !isMe && followService.isFollowing(viewerId, profileId),
                isMe
        );
    }

    public PlayerListDto getFollowing(String username, Long profileId) {
        UserEntity me = userService.getByUsername(username);

        return toPlayerList(
                followService.getFollowing(profileId, DEFAULT_PROFILE_LIMIT),
                followService.countFollowing(profileId),
                me.getId());
    }

    public PlayerListDto getFollowers(String username, Long profileId) {
        UserEntity me = userService.getByUsername(username);

        return toPlayerList(
                followService.getFollowers(profileId, DEFAULT_PROFILE_LIMIT),
                followService.countFollowers(profileId),
                me.getId());
    }

    /**
     * Complète une liste de membres avec ce qui la rend lisible : le nombre de jeux notés et
     * l'état du bouton « suivre ». Les deux viennent d'une requête d'agrégat pour toute la
     * liste, et non d'un appel par ligne affichée.
     */
    private PlayerListDto toPlayerList(List<UserSearchView> views, long total, Long viewerId) {
        if (views.isEmpty()) {
            return new PlayerListDto(List.of(), total);
        }

        List<Long> ids = views.stream().map(UserSearchView::getId).toList();

        Map<Long, Long> ratedGames = gameRatingService.countRatingsByUser(ids);
        Set<Long> followed = followService.getFollowedIdsAmong(viewerId, ids);

        List<UserSearchResultDto> players = views.stream()
                .map(view -> userMapper.toSearchDto(view,
                        ratedGames.getOrDefault(view.getId(), 0L),
                        followed.contains(view.getId())))
                .toList();

        return new PlayerListDto(players, total);
    }
}
