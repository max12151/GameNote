package be.technifutur.dal.comment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameCommentRepository extends JpaRepository<GameCommentEntity, Long> {

    Optional<GameCommentEntity> findByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    @Modifying
    @Query("delete from GameCommentEntity c where c.userId = :userId and c.igdbGameId = :igdbGameId")
    int deleteByUserIdAndIgdbGameId(@Param("userId") Long userId,
                                    @Param("igdbGameId") Long igdbGameId);

    // Le pseudo de l'auteur et sa note sont joints ici plutôt que rechargés commentaire par
    // commentaire côté service. L'avatar, lui, n'est représenté que par un booléen : l'image
    // est une data URI base64 pouvant peser plusieurs mégaoctets, la transporter pour chaque
    // commentaire ferait exploser la réponse. Le front la demande à la route dédiée, qui la
    // sert avec un ETag et laisse donc le navigateur la mettre en cache.
    @Query("""
            select c.id as id,
                   c.igdbGameId as igdbGameId,
                   c.content as content,
                   c.createdAt as createdAt,
                   c.updatedAt as updatedAt,
                   u.id as authorId,
                   u.username as authorUsername,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as authorHasAvatar,
                   r.rating as authorRating
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
                left join GameRatingEntity r on r.userId = c.userId and r.igdbGameId = c.igdbGameId
            where c.igdbGameId = :igdbGameId
            order by c.createdAt desc
            """)
    List<GameCommentView> findViewsByIgdbGameId(@Param("igdbGameId") Long igdbGameId);

    @Query("""
            select c.igdbGameId as igdbGameId, count(c) as commentCount
            from GameCommentEntity c
            where c.igdbGameId in :igdbGameIds
            group by c.igdbGameId
            """)
    List<GameCommentCountView> countByIgdbGameIds(@Param("igdbGameIds") Collection<Long> igdbGameIds);

    /**
     * Derniers commentaires du site, ou ceux d'un seul auteur si {@code authorId} est fourni.
     * <p>
     * Une seule requête pour les deux usages : la clause est neutralisée quand le paramètre
     * est nul, comme le filtre de titre du classement l'est sur une recherche vide. Écrire
     * deux fois la même projection de onze colonnes pour une seule ligne d'écart aurait
     * surtout garanti qu'elles finissent par diverger.
     * <p>
     * La pagination porte le « combien » : l'accueil en veut une poignée, un profil un peu
     * plus, et aucun appelant ne doit ramener toute la table pour en afficher six.
     */
    @Query("""
            select c.id as id,
                   c.igdbGameId as igdbGameId,
                   c.content as content,
                   c.createdAt as createdAt,
                   c.updatedAt as updatedAt,
                   u.id as authorId,
                   u.username as authorUsername,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as authorHasAvatar,
                   r.rating as authorRating,
                   r.title as gameTitle,
                   r.coverUrl as gameCoverUrl
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
                join GameRatingEntity r on r.userId = c.userId and r.igdbGameId = c.igdbGameId
            where :authorId is null or c.userId = :authorId
            order by c.createdAt desc
            """)
    List<RecentGameCommentView> findRecentViews(@Param("authorId") Long authorId, Pageable pageable);
}
