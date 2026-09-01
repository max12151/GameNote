package be.technifutur.dal.rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRatingRepository extends JpaRepository<GameRatingEntity, Long> {

    List<GameRatingEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GameRatingEntity> findByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    boolean existsByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    @Query("select r.igdbGameId from GameRatingEntity r where r.userId = :userId")
    List<Long> findIgdbGameIdsByUserId(@Param("userId") Long userId);

    /**
     * N'importe quelle note du jeu sert de fiche descriptive : les métadonnées (résumé,
     * genres, studios, plateformes) viennent d'IGDB et sont donc identiques d'une note à
     * l'autre.
     * <p>
     * Le tri écarte d'abord les lignes sans résumé — une note enregistrée par un client
     * qui n'envoyait pas encore toutes les métadonnées ne doit pas condamner la fiche à
     * rester vide — puis prend la plus ancienne des restantes, pour un affichage stable
     * dans le temps. L'appelant limite le résultat à une seule ligne.
     */
    @Query("""
            select r
            from GameRatingEntity r
            where r.igdbGameId = :igdbGameId
            order by case when r.summary is null then 1 else 0 end, r.createdAt asc
            """)
    List<GameRatingEntity> findDescriptiveRatings(@Param("igdbGameId") Long igdbGameId, Pageable pageable);

    /**
     * Classement communautaire, paginé côté base : une seule requête agrégée plutôt que
     * de charger toutes les notes du site pour les regrouper en mémoire.
     * <p>
     * Le tri est écrit dans la requête (et non porté par le {@link Pageable}) parce qu'il
     * porte sur des agrégats, que Spring Data ne sait pas préfixer automatiquement : le
     * {@link Pageable} passé ici doit rester non trié et ne fournir que limit/offset.
     */
    @Query("""
            select r.igdbGameId as igdbGameId,
                   max(r.title) as title,
                   max(r.coverUrl) as coverUrl,
                   max(r.releaseDate) as releaseDate,
                   avg(r.rating) as averageRating,
                   count(r) as ratingCount
            from GameRatingEntity r
            where lower(r.title) like lower(concat('%', :search, '%'))
            group by r.igdbGameId
            order by avg(r.rating) desc, count(r) desc, max(r.title) asc
            """)
    List<CommunityGameView> findCommunityRanking(@Param("search") String search, Pageable pageable);

    /**
     * Le filtre porte la recherche et le classement complet à la fois : une recherche vide
     * donne {@code like '%%'}, qui laisse tout passer. Une seule requête à maintenir plutôt
     * que deux variantes qui divergeraient au premier changement de tri.
     */
    @Query("""
            select count(distinct r.igdbGameId)
            from GameRatingEntity r
            where lower(r.title) like lower(concat('%', :search, '%'))
            """)
    long countRatedGames(@Param("search") String search);

    /**
     * Moyennes du site pour une liste de jeux donnée, en une seule requête.
     * <p>
     * Le {@code having count(r) > 1} écarte les jeux que personne d'autre n'a notés : leur
     * « moyenne du site » serait la note de l'utilisateur lui-même, et les comparer
     * reviendrait à mesurer un écart nul artificiel.
     */
    @Query("""
            select r.igdbGameId as igdbGameId,
                   max(r.title) as title,
                   max(r.coverUrl) as coverUrl,
                   max(r.releaseDate) as releaseDate,
                   avg(r.rating) as averageRating,
                   count(r) as ratingCount
            from GameRatingEntity r
            where r.igdbGameId in :igdbGameIds
            group by r.igdbGameId
            having count(r) > 1
            """)
    List<CommunityGameView> findCommunityAveragesFor(@Param("igdbGameIds") Collection<Long> igdbGameIds);

    /**
     * Histogramme des notes d'un jeu. La moyenne et le total s'en déduisent, ce qui
     * évite une seconde requête d'agrégation pour la fiche détaillée.
     */
    @Query("""
            select r.rating as rating, count(r) as count
            from GameRatingEntity r
            where r.igdbGameId = :igdbGameId
            group by r.rating
            order by r.rating
            """)
    List<RatingBucketView> findRatingDistribution(@Param("igdbGameId") Long igdbGameId);
}
