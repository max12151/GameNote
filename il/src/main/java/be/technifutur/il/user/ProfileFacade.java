package be.technifutur.il.user;

import be.technifutur.bll.comment.GameCommentService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.AvatarImage;
import be.technifutur.bll.user.AvatarService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.user.PublicProfileDto;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.comment.GameCommentMapper;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProfileFacade {

    /** Assez pour donner le ton d'un joueur sans transformer son profil en fil sans fin. */
    private static final int PROFILE_COMMENTS = 10;

    private final UserService userService;
    private final AvatarService avatarService;
    private final GameRatingService gameRatingService;
    private final GameCommentService gameCommentService;
    private final UserMapper userMapper;
    private final GameCommentMapper gameCommentMapper;

    public ProfileFacade(UserService userService,
                         AvatarService avatarService,
                         GameRatingService gameRatingService,
                         GameCommentService gameCommentService,
                         UserMapper userMapper,
                         GameCommentMapper gameCommentMapper) {
        this.userService = userService;
        this.avatarService = avatarService;
        this.gameRatingService = gameRatingService;
        this.gameCommentService = gameCommentService;
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
     * Profil d'un joueur consulté par un autre membre : ses statistiques de notation et
     * ses derniers avis, sans rien de ce qui relève de son compte.
     * <p>
     * {@link Optional#empty()} pour un identifiant inconnu : il vient d'une URL que
     * n'importe qui peut taper, ce n'est pas une anomalie mais un 404 ordinaire.
     */
    public Optional<PublicProfileDto> getPublicProfile(Long userId) {
        return userService.findById(userId).map(user -> {
            List<RecentCommentDto> comments =
                    gameCommentService.getCommentsByAuthor(user.getId(), PROFILE_COMMENTS).stream()
                            .map(gameCommentMapper::toRecentDto)
                            .toList();

            return userMapper.toPublicDto(user, gameRatingService.getStats(user.getId()), comments);
        });
    }
}
