package be.technifutur.il.user;


import be.technifutur.bll.rating.RatingStats;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.user.PublicProfileDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.rating.GameRatingMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    private final GameRatingMapper gameRatingMapper;

    public UserMapper(GameRatingMapper gameRatingMapper) {
        this.gameRatingMapper = gameRatingMapper;
    }

    public UserDto toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(entity.getId());
        dto.setUsername(entity.getUsername());
        dto.setEmail(entity.getEmail());
        dto.setAvatarUrl(entity.getAvatarUrl());
        dto.setBio(entity.getBio());
        dto.setRole(entity.getRole().name());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    /**
     * Profil d'un joueur vu par un autre membre.
     * <p>
     * Ni l'adresse e-mail ni l'avatar en base64 ne sont recopiés : le premier n'a rien à
     * faire hors du compte de son propriétaire, le second pèse jusqu'à deux mégaoctets et
     * est déjà servi par une route dédiée que le navigateur met en cache. Seul un booléen
     * dit à la pastille d'identité s'il y a une image à demander.
     * <p>
     * La comparaison de goûts, elle, reste sur le profil personnel : c'est un retour
     * adressé à quelqu'un sur sa propre façon de noter, pas une donnée à exposer.
     */
    public PublicProfileDto toPublicDto(UserEntity entity,
                                        RatingStats stats,
                                        List<RecentCommentDto> recentComments) {
        return new PublicProfileDto(
                entity.getId(),
                entity.getUsername(),
                entity.getBio(),
                entity.getAvatarUrl() != null && !entity.getAvatarUrl().isEmpty(),
                entity.getRole().name(),
                entity.getCreatedAt(),
                stats.totalRated(),
                stats.averageRating(),
                stats.topGenre(),
                stats.topGenreCount(),
                gameRatingMapper.toDto(stats.bestRatedGame()),
                stats.genreBreakdown().stream().map(gameRatingMapper::toGenreCountDto).toList(),
                recentComments
        );
    }
}
