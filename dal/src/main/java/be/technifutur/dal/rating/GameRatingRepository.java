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
     * L'ordre est donné par une moyenne pondérée bayésienne, celle qu'emploie IMDb :
     * <pre>score = (v × R + m × C) / (v + m)</pre>
     * où {@code R} est la moyenne du jeu, {@code v} son nombre de votes, {@code C} la
     * moyenne du site et {@code m} le nombre de votes à partir duquel on prend une note au
     * sérieux.
     * <p>
     * Trier sur la moyenne brute plaçait en tête les jeux notés une seule fois : un 10
     * unique battait un 9,2 confirmé par onze joueurs. La formule ramène chaque jeu vers la
     * moyenne du site à proportion de ce qui lui manque de votes — un jeu à {@code v = m}
     * est à mi-chemin entre sa moyenne et celle du site, et l'attraction s'efface à mesure
     * que les votes s'accumulent. Aucun jeu n'est écarté, ce qu'un simple seuil
     * « minimum {@code m} votes » aurait fait.
     * <p>
     * Le tri est écrit dans la requête (et non porté par le {@link Pageable}) parce qu'il
     * porte sur des agrégats, que Spring Data ne sait pas préfixer automatiquement : le
     * {@link Pageable} passé ici doit rester non trié et ne fournir que limit/offset.
     *
     * Les {@code cast(... as Double)} ne sont pas décoratifs : sans eux, Hibernate déduit le
     * type des paramètres de leur voisinage — un {@code count(r)} entier — et rejette à
     * l'exécution toute valeur décimale, alors même que la requête est acceptée au démarrage.
     *
     * @param globalAverage moyenne du site, le {@code C} de la formule
     * @param minimumVotes  seuil de votes, le {@code m} de la formule ; passé en flottant
     *                      pour que la division reste réelle
     */
    @Query("""
            select r.igdbGameId as igdbGameId,
                   max(r.title) as title,
                   max(r.coverUrl) as coverUrl,
                   max(r.releaseDate) as releaseDate,
                   avg(r.rating) as averageRating,
                   count(r) as ratingCount,
                   (count(r) * avg(r.rating) + cast(:minimumVotes as Double) * cast(:globalAverage as Double))
                       / (count(r) + cast(:minimumVotes as Double)) as weightedRating
            from GameRatingEntity r
            where lower(r.title) like lower(concat('%', :search, '%'))
            group by r.igdbGameId
            order by weightedRating desc, count(r) desc, max(r.title) asc
            """)
    List<CommunityRankingView> findCommunityRanking(@Param("search") String search,
                                                    @Param("globalAverage") double globalAverage,
                                                    @Param("minimumVotes") double minimumVotes,
                                                    Pageable pageable);

    /**
     * Un couple (votes, moyenne) par jeu noté, pour établir les deux constantes de la
     * pondération : la moyenne du site et la médiane des votes.
     * <p>
     * La recherche n'entre pas ici, volontairement. Ces constantes décrivent le classement
     * entier ; les calculer sur les seuls résultats affichés ferait varier le score d'un
     * jeu selon ce qui a été tapé dans la barre de recherche.
     */
    @Query("""
            select count(r) as ratingCount, avg(r.rating) as averageRating
            from GameRatingEntity r
            group by r.igdbGameId
            """)
    List<GameVoteView> findVotesPerGame();

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
