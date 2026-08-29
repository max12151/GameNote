package be.technifutur.dal.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    /**
     * Ne remonte que la colonne de l'avatar : c'est une data URI base64 qui peut peser
     * plusieurs mégaoctets, inutile de charger l'utilisateur entier pour la servir.
     */
    @Query("select u.avatarUrl from UserEntity u where u.id = :id")
    Optional<String> findAvatarUrlById(@Param("id") Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}