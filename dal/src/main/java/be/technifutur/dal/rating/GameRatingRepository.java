package be.technifutur.dal.rating;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Depuis l'arrivée des statuts, une ligne de cette table est une <em>entrée de
 * bibliothèque</em> : le jeu y figure parce qu'un joueur l'a rangé quelque part, avec ou sans
 * note. Toutes les agrégations du classement portent donc un {@code r.rating is not null}
 * explicite — sans lui, un jeu que dix joueurs veulent essayer compterait dix votes.
 */
@Repository
public interface GameRatingRepository extends JpaRepository<GameRatingEntity, Long> {

    List<GameRatingEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GameRatingEntity> findByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    /**
     * Le jeu est-il réellement <em>noté</em> par ce joueur ?
     * <p>
     * La simple présence dans la bibliothèque ne suffit pas : depuis les statuts, une entrée
     * peut exister sans note. Commenter reste conditionné à une note — un avis sans note
     * affichée à côté n'aurait rien à quoi se rattacher.
     */
    @Query("""
            select count(r) > 0
            from GameRatingEntity r
            where r.userId = :userId and r.igdbGameId = :igdbGameId and r.rating is not null
            """)
    boolean existsRatedByUserIdAndIgdbGameId(@Param("userId") Long userId,
                                             @Param("igdbGameId") Long igdbGameId);

    @Query("select r.igdbGameId from GameRatingEntity r where r.userId = :userId")
    List<Long> findIgdbGameIdsByUserId(@Param("userId") Long userId);

    /** La bibliothèque d'un joueur restreinte à un statut : ses envies, ses jeux en cours. */
    List<GameRatingEntity> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, GameStatus status);

    /**
     * Combien de jeux ce joueur a rangés dans chaque statut, en une requête. Un statut jamais
     * employé n'a pas de ligne : c'est à l'appelant de lire zéro.
     */
    @Query("""
            select r.status as status, count(r) as count
            from GameRatingEntity r
            where r.userId = :userId
            group by r.status
            """)
    List<StatusCountView> countByStatusForUser(@Param("userId") Long userId);

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
     * Classement communautaire, paginé, filtré et trié côté base : une seule requête agrégée
     * plutôt que de charger toutes les notes du site pour les regrouper en mémoire.
     * <p>
     * L'ordre par défaut est donné par une moyenne pondérée bayésienne, celle qu'emploie
     * IMDb :
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
     * <strong>Filtres.</strong> Genre et plateforme passent par un {@code exists} et non par
     * une jointure sur la collection : joindre {@code r.genres} multiplierait chaque ligne de
     * note par son nombre de genres, et un jeu à cinq genres compterait cinq votes. Un
     * paramètre nul neutralise son filtre, comme la recherche vide.
     * <p>
     * Les {@code cast(... as String)} ne sont pas décoratifs, comme ceux de la formule :
     * PostgreSQL refuse un paramètre dont il ne peut pas déduire le type, et un filtre absent
     * arrive précisément comme un paramètre nul. Sans eux, la première page du classement
     * échouait sur un {@code function lower(bytea) does not exist} — la base, faute de mieux,
     * devinait « suite d'octets » là où on lui passait un nom de genre.
     * <p>
     * <strong>Tri.</strong> Le nom du tri arrive en paramètre et un {@code case} choisit la
     * valeur ordonnée. Cinq requêtes jumelles ne différant que par leur {@code order by}
     * auraient fini par diverger de la clause de filtrage, et une clause construite par
     * concaténation en Java aurait ouvert la porte à l'injection. Une valeur inconnue
     * retombe sur le score pondéré, l'ordre par défaut du site.
     * <p>
     * Le {@link Pageable} passé ici doit rester non trié et ne fournir que limit/offset : le
     * tri porte sur des agrégats, que Spring Data ne sait pas préfixer automatiquement.
     *
     * @param globalAverage  moyenne du site, le {@code C} de la formule
     * @param minimumVotes   seuil de votes, le {@code m} de la formule ; passé en flottant
     *                       pour que la division reste réelle
     * @param sort           WEIGHTED (défaut), AVERAGE, VOTES, RECENT ou TITLE
     * @param releasedAfter  borne basse de date de sortie, en secondes Unix, ou null
     * @param releasedBefore borne haute de date de sortie, en secondes Unix, ou null
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
            where r.rating is not null
              and lower(r.title) like lower(concat('%', :search, '%'))
              and (cast(:genre as String) is null or exists (
                    select 1 from GameRatingEntity g join g.genres genre
                    where g.igdbGameId = r.igdbGameId and lower(genre) = lower(cast(:genre as String))))
              and (cast(:platform as String) is null or exists (
                    select 1 from GameRatingEntity p join p.platforms platform
                    where p.igdbGameId = r.igdbGameId and lower(platform) = lower(cast(:platform as String))))
              and (cast(:releasedAfter as Long) is null or r.releaseDate >= :releasedAfter)
              and (cast(:releasedBefore as Long) is null or r.releaseDate <= :releasedBefore)
            group by r.igdbGameId
            order by
                case when :sort = 'AVERAGE' then avg(r.rating)
                     when :sort = 'VOTES'   then cast(count(r) as Double)
                     when :sort = 'RECENT'  then cast(coalesce(max(r.releaseDate), 0L) as Double)
                     when :sort = 'TITLE'   then cast(0 as Double)
                     else (count(r) * avg(r.rating) + cast(:minimumVotes as Double) * cast(:globalAverage as Double))
                              / (count(r) + cast(:minimumVotes as Double))
                end desc,
                case when :sort = 'TITLE' then lower(max(r.title)) else '' end asc,
                count(r) desc,
                lower(max(r.title)) asc
            """)
    List<CommunityRankingView> findCommunityRanking(@Param("search") String search,
                                                    @Param("genre") String genre,
                                                    @Param("platform") String platform,
                                                    @Param("releasedAfter") Long releasedAfter,
                                                    @Param("releasedBefore") Long releasedBefore,
                                                    @Param("sort") String sort,
                                                    @Param("globalAverage") double globalAverage,
                                                    @Param("minimumVotes") double minimumVotes,
                                                    Pageable pageable);

    /**
     * Nombre de jeux du classement, filtres compris — c'est ce total qui dit au front combien
     * de pages proposer. Les mêmes clauses que {@link #findCommunityRanking} : compter sans
     * filtrer annoncerait des pages vides.
     */
    @Query("""
            select count(distinct r.igdbGameId)
            from GameRatingEntity r
            where r.rating is not null
              and lower(r.title) like lower(concat('%', :search, '%'))
              and (cast(:genre as String) is null or exists (
                    select 1 from GameRatingEntity g join g.genres genre
                    where g.igdbGameId = r.igdbGameId and lower(genre) = lower(cast(:genre as String))))
              and (cast(:platform as String) is null or exists (
                    select 1 from GameRatingEntity p join p.platforms platform
                    where p.igdbGameId = r.igdbGameId and lower(platform) = lower(cast(:platform as String))))
              and (cast(:releasedAfter as Long) is null or r.releaseDate >= :releasedAfter)
              and (cast(:releasedBefore as Long) is null or r.releaseDate <= :releasedBefore)
            """)
    long countRatedGames(@Param("search") String search,
                         @Param("genre") String genre,
                         @Param("platform") String platform,
                         @Param("releasedAfter") Long releasedAfter,
                         @Param("releasedBefore") Long releasedBefore);

    /**
     * Un couple (votes, moyenne) par jeu noté, pour établir les deux constantes de la
     * pondération : la moyenne du site et la médiane des votes.
     * <p>
     * Ni la recherche ni les filtres n'entrent ici, volontairement. Ces constantes décrivent
     * le classement entier ; les calculer sur les seuls résultats affichés ferait varier le
     * score d'un jeu selon le genre coché dans la barre de filtres.
     */
    @Query("""
            select count(r) as ratingCount, avg(r.rating) as averageRating
            from GameRatingEntity r
            where r.rating is not null
            group by r.igdbGameId
            """)
    List<GameVoteView> findVotesPerGame();

    /** Les genres présents dans le classement, pour peupler le filtre sans les inventer. */
    @Query("""
            select distinct genre
            from GameRatingEntity r join r.genres genre
            where r.rating is not null
            order by genre asc
            """)
    List<String> findDistinctGenres();

    /** Idem pour les plateformes. */
    @Query("""
            select distinct platform
            from GameRatingEntity r join r.platforms platform
            where r.rating is not null
            order by platform asc
            """)
    List<String> findDistinctPlatforms();

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
            where r.igdbGameId in :igdbGameIds and r.rating is not null
            group by r.igdbGameId
            having count(r) > 1
            """)
    List<CommunityGameView> findCommunityAveragesFor(@Param("igdbGameIds") Collection<Long> igdbGameIds);

    /**
     * Nombre de jeux notés par utilisateur, en une requête pour toute une liste de
     * membres — les résultats d'une recherche, par exemple. Un membre qui n'a rien noté
     * n'a pas de ligne : c'est à l'appelant de compléter par zéro.
     */
    @Query("""
            select r.userId as userId, count(r) as ratedGames
            from GameRatingEntity r
            where r.userId in :userIds and r.rating is not null
            group by r.userId
            """)
    List<UserRatingCountView> countRatingsByUserIds(@Param("userIds") Collection<Long> userIds);

    /**
     * Histogramme des notes d'un jeu. La moyenne et le total s'en déduisent, ce qui
     * évite une seconde requête d'agrégation pour la fiche détaillée.
     */
    @Query("""
            select r.rating as rating, count(r) as count
            from GameRatingEntity r
            where r.igdbGameId = :igdbGameId and r.rating is not null
            group by r.rating
            order by r.rating
            """)
    List<RatingBucketView> findRatingDistribution(@Param("igdbGameId") Long igdbGameId);

    /**
     * Les dernières notes des joueurs suivis, pour le fil d'activité.
     * <p>
     * L'ordre se fait sur la date de dernière modification : un jeu noté aujourd'hui après un
     * an passé dans la liste d'envies est une nouvelle d'aujourd'hui, pas d'il y a un an. Les
     * comptes anonymisés sont écartés — leur profil n'existe plus, la carte ne mènerait nulle
     * part.
     */
    @Query("""
            select r.id as id,
                   r.igdbGameId as igdbGameId,
                   r.title as gameTitle,
                   r.coverUrl as gameCoverUrl,
                   r.rating as rating,
                   coalesce(r.updatedAt, r.createdAt) as activityAt,
                   u.id as authorId,
                   u.username as authorUsername,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as authorHasAvatar
            from GameRatingEntity r
                join UserEntity u on u.id = r.userId
            where r.userId in :userIds and r.rating is not null
              and u.deletedAt is null and u.suspendedAt is null
            order by coalesce(r.updatedAt, r.createdAt) desc
            """)
    List<RatingActivityView> findActivityForUsers(@Param("userIds") Collection<Long> userIds, Pageable pageable);

    /** Tableau de bord de la console : combien de notes, sur combien de jeux distincts. */
    @Query("select count(r) from GameRatingEntity r where r.rating is not null")
    long countRatings();

    @Query("select count(distinct r.igdbGameId) from GameRatingEntity r where r.rating is not null")
    long countDistinctRatedGames();

    @Query("select avg(r.rating) from GameRatingEntity r where r.rating is not null")
    Double findGlobalAverageRating();
}
