package be.technifutur.dal.list;

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
public interface GameListItemRepository extends JpaRepository<GameListItemEntity, Long> {

    List<GameListItemEntity> findByGameListIdOrderBySortIndexAsc(Long gameListId);

    Optional<GameListItemEntity> findByGameListIdAndIgdbGameId(Long gameListId, Long igdbGameId);

    boolean existsByGameListIdAndIgdbGameId(Long gameListId, Long igdbGameId);

    long countByGameListId(Long gameListId);

    @Modifying
    @Query("delete from GameListItemEntity i where i.gameListId = :gameListId")
    int deleteByGameListId(@Param("gameListId") Long gameListId);

    /** Vide plusieurs listes d'un coup : la suppression d'un compte, qui les emporte toutes. */
    @Modifying
    @Query("delete from GameListItemEntity i where i.gameListId in :gameListIds")
    int deleteByGameListIds(@Param("gameListIds") Collection<Long> gameListIds);

    /**
     * Rang le plus élevé déjà utilisé dans la liste, pour ajouter le jeu suivant à la fin.
     * Renvoie {@code null} sur une liste vide, d'où le type objet.
     */
    @Query("select max(i.sortIndex) from GameListItemEntity i where i.gameListId = :gameListId")
    Integer findMaxSortIndex(@Param("gameListId") Long gameListId);

    /**
     * Les premières jaquettes d'une liste, pour la vignette de l'index. La pagination porte
     * le « combien » : l'appelant en demande quatre, pas la liste entière.
     */
    @Query("""
            select i.coverUrl
            from GameListItemEntity i
            where i.gameListId = :gameListId and i.coverUrl is not null
            order by i.sortIndex asc
            """)
    List<String> findCovers(@Param("gameListId") Long gameListId, Pageable pageable);

    /**
     * Les premières jaquettes de plusieurs listes à la fois, pour dessiner les vignettes d'un
     * index entier en une requête.
     * <p>
     * En SQL natif, seul endroit du projet dans ce cas : il faut une fonction de fenêtrage
     * pour ne garder que les {@code maxPerList} premières lignes <em>par liste</em>, et JPQL
     * n'en propose pas. L'écrire en JPQL aurait voulu dire ramener toutes les jaquettes de
     * toutes les listes affichées pour n'en garder que quatre de chacune.
     */
    @Query(value = """
            select game_list_id as gameListId, cover_url as coverUrl
            from (select game_list_id,
                         cover_url,
                         row_number() over (partition by game_list_id order by sort_index) as rang
                  from game_list_item
                  where game_list_id in (:gameListIds) and cover_url is not null) classees
            where rang <= :maxPerList
            """, nativeQuery = true)
    List<ListCoverView> findCoverPreviews(@Param("gameListIds") Collection<Long> gameListIds,
                                          @Param("maxPerList") int maxPerList);

    /** Dans quelles listes de ce membre ce jeu figure-t-il déjà ? */
    @Query("""
            select i.gameListId
            from GameListItemEntity i
                join GameListEntity l on l.id = i.gameListId
            where l.userId = :userId and i.igdbGameId = :igdbGameId
            """)
    List<Long> findListIdsContaining(@Param("userId") Long userId,
                                     @Param("igdbGameId") Long igdbGameId);
}
