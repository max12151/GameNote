package be.technifutur.dal.list;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameListRepository extends JpaRepository<GameListEntity, Long> {

    /**
     * Les listes d'un membre.
     * <p>
     * {@code includePrivate} distingue les deux lectures possibles : le propriétaire voit
     * tout, un visiteur seulement le public. Un seul paramètre plutôt que deux requêtes
     * jumelles, qui auraient fini par diverger au premier changement de tri.
     */
    @Query("""
            select l.id as id,
                   l.name as name,
                   l.description as description,
                   l.visibility as visibility,
                   l.createdAt as createdAt,
                   l.updatedAt as updatedAt,
                   u.id as ownerId,
                   u.username as ownerUsername,
                   (select count(i) from GameListItemEntity i where i.gameListId = l.id) as itemCount
            from GameListEntity l
                join UserEntity u on u.id = l.userId
            where l.userId = :userId
              and (:includePrivate = true or l.visibility = be.technifutur.dal.list.ListVisibility.PUBLIC)
            order by coalesce(l.updatedAt, l.createdAt) desc
            """)
    List<GameListSummaryView> findByOwner(@Param("userId") Long userId,
                                          @Param("includePrivate") boolean includePrivate,
                                          Pageable pageable);

    /** Les dernières listes publiées sur le site, pour la vitrine de l'index des listes. */
    @Query("""
            select l.id as id,
                   l.name as name,
                   l.description as description,
                   l.visibility as visibility,
                   l.createdAt as createdAt,
                   l.updatedAt as updatedAt,
                   u.id as ownerId,
                   u.username as ownerUsername,
                   (select count(i) from GameListItemEntity i where i.gameListId = l.id) as itemCount
            from GameListEntity l
                join UserEntity u on u.id = l.userId
            where l.visibility = be.technifutur.dal.list.ListVisibility.PUBLIC
              and u.deletedAt is null
              and u.suspendedAt is null
              and lower(l.name) like lower(concat('%', :search, '%'))
            order by l.createdAt desc
            """)
    List<GameListSummaryView> findPublicLists(@Param("search") String search, Pageable pageable);

    @Query("""
            select count(l)
            from GameListEntity l
                join UserEntity u on u.id = l.userId
            where l.visibility = be.technifutur.dal.list.ListVisibility.PUBLIC
              and u.deletedAt is null
              and u.suspendedAt is null
              and lower(l.name) like lower(concat('%', :search, '%'))
            """)
    long countPublicLists(@Param("search") String search);

    /** Une liste avec l'identité de son auteur, sans requête supplémentaire pour le pseudo. */
    @Query("""
            select l.id as id,
                   l.name as name,
                   l.description as description,
                   l.visibility as visibility,
                   l.createdAt as createdAt,
                   l.updatedAt as updatedAt,
                   u.id as ownerId,
                   u.username as ownerUsername,
                   (select count(i) from GameListItemEntity i where i.gameListId = l.id) as itemCount
            from GameListEntity l
                join UserEntity u on u.id = l.userId
            where l.id = :id
            """)
    Optional<GameListSummaryView> findSummaryById(@Param("id") Long id);

    long countByUserId(Long userId);

    @Query("select l.id from GameListEntity l where l.userId = :userId")
    List<Long> findIdsByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from GameListEntity l where l.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
