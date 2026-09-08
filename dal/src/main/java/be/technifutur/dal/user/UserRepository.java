package be.technifutur.dal.user;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    /**
     * Le strict nécessaire pour authentifier une requête : identifiant, pseudo, rôle et état
     * du compte.
     * <p>
     * Cette lecture a lieu à chaque requête entrante. Charger l'entité complète y ferait
     * passer l'avatar — une data URI base64 de plusieurs mégaoctets — par le réseau et par la
     * mémoire, pour lire un rôle.
     */
    @Query("""
            select u.id as id,
                   u.username as username,
                   u.role as role,
                   u.suspendedAt as suspendedAt,
                   u.deletedAt as deletedAt
            from UserEntity u
            where u.username = :username
            """)
    Optional<UserAuthView> findAuthByUsername(@Param("username") String username);

    /**
     * Ne remonte que la colonne de l'avatar : c'est une data URI base64 qui peut peser
     * plusieurs mégaoctets, inutile de charger l'utilisateur entier pour la servir.
     */
    @Query("select u.avatarUrl from UserEntity u where u.id = :id")
    Optional<String> findAvatarUrlById(@Param("id") Long id);

    /**
     * Membres dont le pseudo contient le terme cherché.
     * <p>
     * Le filtrage est fait par la base et la pagination porte le « combien » : une
     * recherche sur une lettre ne doit pas pouvoir ramener la table entière.
     * <p>
     * L'ordre place d'abord les pseudos qui <em>commencent</em> par le terme — chercher
     * « max » doit donner Max avant Maxwell et avant Klimax — puis départage
     * alphabétiquement, pour que deux recherches identiques donnent le même ordre.
     * <p>
     * Les comptes anonymisés sont écartés : leur profil n'existe plus, une carte cliquable
     * ne mènerait nulle part.
     */
    @Query("""
            select u.id as id,
                   u.username as username,
                   case when u.avatarUrl is null or length(u.avatarUrl) = 0 then false else true end as hasAvatar
            from UserEntity u
            where lower(u.username) like lower(concat('%', :search, '%'))
              and u.deletedAt is null
              and u.suspendedAt is null
            order by case when lower(u.username) like lower(concat(:search, '%')) then 0 else 1 end,
                     lower(u.username) asc
            """)
    List<UserSearchView> searchByUsername(@Param("search") String search, Pageable pageable);

    /**
     * Table des comptes de la console d'administration, comptes anonymisés compris : c'est là
     * qu'on constate qu'une suppression a bien eu lieu.
     * <p>
     * Les deux compteurs viennent de sous-requêtes portées par la requête principale — une
     * page de vingt comptes coûte une requête, pas quarante-et-une. Le comptage des jeux
     * notés ignore les entrées de bibliothèque sans note : une envie n'est pas un avis.
     */
    @Query("""
            select u.id as id,
                   u.username as username,
                   u.email as email,
                   u.role as role,
                   u.createdAt as createdAt,
                   u.suspendedAt as suspendedAt,
                   u.suspensionReason as suspensionReason,
                   u.deletedAt as deletedAt,
                   (select count(r) from GameRatingEntity r
                     where r.userId = u.id and r.rating is not null) as ratedGames,
                   (select count(c) from GameCommentEntity c where c.userId = u.id) as comments
            from UserEntity u
            where lower(u.username) like lower(concat('%', :search, '%'))
            order by u.createdAt desc
            """)
    List<UserAdminView> findAdminUsers(@Param("search") String search, Pageable pageable);

    @Query("select count(u) from UserEntity u where lower(u.username) like lower(concat('%', :search, '%'))")
    long countAdminUsers(@Param("search") String search);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /**
     * Variante employée au changement d'e-mail : réutiliser sa propre adresse ne doit pas
     * être refusé comme un doublon.
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    /** Comptes actifs, pour le tableau de bord : ni suspendus, ni anonymisés. */
    long countBySuspendedAtIsNullAndDeletedAtIsNull();

    long countBySuspendedAtIsNotNull();

    @Query("select count(u) from UserEntity u where u.createdAt >= :since and u.deletedAt is null")
    long countRegisteredSince(@Param("since") java.time.OffsetDateTime since);
}
