package be.technifutur.dal.follow;

import be.technifutur.dal.user.UserSearchView;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserFollowRepository extends JpaRepository<UserFollowEntity, Long> {

    boolean existsByFollowerIdAndFollowedId(Long followerId, Long followedId);

    @Modifying
    @Query("delete from UserFollowEntity f where f.followerId = :followerId and f.followedId = :followedId")
    int deleteFollow(@Param("followerId") Long followerId, @Param("followedId") Long followedId);

    /** Identifiants des joueurs suivis : c'est de là que part la construction du fil d'activité. */
    @Query("select f.followedId from UserFollowEntity f where f.followerId = :followerId")
    List<Long> findFollowedIds(@Param("followerId") Long followerId);

    /**
     * Coupe tous les liens de ce membre, dans les deux sens : il ne suit plus personne et
     * personne ne le suit. Employé à la suppression d'un compte, dont le lien social n'a plus
     * d'objet une fois l'identité effacée.
     */
    @Modifying
    @Query("delete from UserFollowEntity f where f.followerId = :userId or f.followedId = :userId")
    int deleteAllInvolving(@Param("userId") Long userId);

    long countByFollowerId(Long followerId);

    long countByFollowedId(Long followedId);

    /**
     * Les comptes que ce membre suit, présentés comme des résultats de recherche pour que la
     * même carte serve partout. Les comptes anonymisés sont écartés : leur profil n'existe
     * plus, une carte cliquable ne mènerait nulle part.
     */
    @Query("""
            select u.id as id,
                   u.username as username,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as hasAvatar
            from UserFollowEntity f
                join UserEntity u on u.id = f.followedId
            where f.followerId = :followerId and u.deletedAt is null and u.suspendedAt is null
            order by lower(u.username) asc
            """)
    List<UserSearchView> findFollowedProfiles(@Param("followerId") Long followerId, Pageable pageable);

    /** Symétrique de {@link #findFollowedProfiles} : qui suit ce membre. */
    @Query("""
            select u.id as id,
                   u.username as username,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as hasAvatar
            from UserFollowEntity f
                join UserEntity u on u.id = f.followerId
            where f.followedId = :followedId and u.deletedAt is null and u.suspendedAt is null
            order by lower(u.username) asc
            """)
    List<UserSearchView> findFollowerProfiles(@Param("followedId") Long followedId, Pageable pageable);

    /**
     * Parmi ces membres, lesquels sont déjà suivis. Une requête pour toute une liste de
     * résultats de recherche, plutôt qu'un « est-ce que je le suis ? » par ligne affichée.
     */
    @Query("""
            select f.followedId
            from UserFollowEntity f
            where f.followerId = :followerId and f.followedId in :candidateIds
            """)
    List<Long> findFollowedIdsAmong(@Param("followerId") Long followerId,
                                    @Param("candidateIds") Collection<Long> candidateIds);
}
