package be.technifutur.dal.discover;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameSkipRepository extends JpaRepository<GameSkipEntity, Long> {

    Optional<GameSkipEntity> findByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    // On ne remonte que les identifiants (et pas les entités) : c'est tout ce dont
    // la page Découvrir a besoin pour filtrer, et ça évite de charger des lignes entières.
    @Query("""
            select s.igdbGameId
            from GameSkipEntity s
            where s.userId = :userId and s.skippedAt > :since
            """)
    List<Long> findIgdbGameIdsSkippedSince(@Param("userId") Long userId,
                                           @Param("since") OffsetDateTime since);

    @Modifying
    @Query("""
            delete from GameSkipEntity s
            where s.userId = :userId and s.skippedAt <= :cutoff
            """)
    int deleteExpiredForUser(@Param("userId") Long userId,
                             @Param("cutoff") OffsetDateTime cutoff);
}
