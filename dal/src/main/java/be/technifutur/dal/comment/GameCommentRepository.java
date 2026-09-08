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

    // Le pseudo de l'auteur, sa note et le nombre de « utile » sont joints ici plutôt que
    // rechargés commentaire par commentaire côté service. L'avatar, lui, n'est représenté que
    // par un booléen : l'image est une data URI base64 pouvant peser plusieurs mégaoctets, la
    // transporter pour chaque commentaire ferait exploser la réponse. Le front la demande à
    // la route dédiée, qui la sert avec un ETag et laisse donc le navigateur la cacher.
    //
    // Le tri arrive en paramètre : « les plus utiles » remonte les avis que la communauté a
    // distingués, « les plus récents » — le défaut — garde le fil chronologique. Un compteur
    // à zéro pour tout le monde quand le tri est chronologique, et c'est la date qui tranche.
    @Query("""
            select c.id as id,
                   c.igdbGameId as igdbGameId,
                   c.content as content,
                   c.createdAt as createdAt,
                   c.updatedAt as updatedAt,
                   u.id as authorId,
                   u.username as authorUsername,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as authorHasAvatar,
                   case when u.deletedAt is null then false else true end as authorDeleted,
                   r.rating as authorRating,
                   (select count(re) from CommentReactionEntity re where re.commentId = c.id) as usefulCount
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
                left join GameRatingEntity r on r.userId = c.userId and r.igdbGameId = c.igdbGameId
            where c.igdbGameId = :igdbGameId and u.suspendedAt is null
            order by
                case when :sort = 'USEFUL'
                     then (select count(re) from CommentReactionEntity re where re.commentId = c.id)
                     else 0L end desc,
                c.createdAt desc
            """)
    List<GameCommentView> findViewsByIgdbGameId(@Param("igdbGameId") Long igdbGameId,
                                                @Param("sort") String sort);

    /**
     * Nombre d'avis par jeu, tel qu'il s'affiche sur le classement.
     * <p>
     * La jointure sur l'auteur n'est pas décorative : le fil d'un jeu masque les avis des
     * comptes suspendus, et un compteur qui les inclurait annoncerait douze avis pour une
     * page qui n'en montre que onze.
     */
    @Query("""
            select c.igdbGameId as igdbGameId, count(c) as commentCount
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
            where c.igdbGameId in :igdbGameIds and u.suspendedAt is null
            group by c.igdbGameId
            """)
    List<GameCommentCountView> countByIgdbGameIds(@Param("igdbGameIds") Collection<Long> igdbGameIds);

    /**
     * Derniers commentaires du site, ou ceux d'un seul auteur si {@code authorId} est fourni.
     * <p>
     * Une seule requête pour les deux usages : la clause est neutralisée quand le paramètre
     * est nul, comme le filtre de titre du classement l'est sur une recherche vide. Écrire
     * deux fois la même projection de douze colonnes pour une seule ligne d'écart aurait
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
                   case when u.deletedAt is null then false else true end as authorDeleted,
                   r.rating as authorRating,
                   (select count(re) from CommentReactionEntity re where re.commentId = c.id) as usefulCount,
                   r.title as gameTitle,
                   r.coverUrl as gameCoverUrl
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
                join GameRatingEntity r on r.userId = c.userId and r.igdbGameId = c.igdbGameId
            where (:authorId is null or c.userId = :authorId) and u.suspendedAt is null
            order by c.createdAt desc
            """)
    List<RecentGameCommentView> findRecentViews(@Param("authorId") Long authorId, Pageable pageable);

    /**
     * Les derniers avis des joueurs suivis, pour le fil d'activité.
     * <p>
     * Même projection que {@link #findRecentViews}, mais sur un ensemble d'auteurs plutôt que
     * sur un seul : passer une collection au paramètre unique aurait demandé une clause
     * {@code in} là où l'autre veut une égalité, et mélanger les deux dans une requête déjà
     * neutralisable par null aurait rendu la clause illisible.
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
                   case when u.deletedAt is null then false else true end as authorDeleted,
                   r.rating as authorRating,
                   (select count(re) from CommentReactionEntity re where re.commentId = c.id) as usefulCount,
                   r.title as gameTitle,
                   r.coverUrl as gameCoverUrl
            from GameCommentEntity c
                join UserEntity u on u.id = c.userId
                join GameRatingEntity r on r.userId = c.userId and r.igdbGameId = c.igdbGameId
            where c.userId in :authorIds and u.deletedAt is null and u.suspendedAt is null
            order by c.createdAt desc
            """)
    List<RecentGameCommentView> findFeedViews(@Param("authorIds") Collection<Long> authorIds, Pageable pageable);

    /**
     * Combien d'avis un joueur a publiés. Sert au front à savoir s'il reste des pages à
     * charger sur un profil, sans lui faire deviner à partir de ce qu'il a déjà reçu.
     */
    long countByUserId(Long userId);
}
