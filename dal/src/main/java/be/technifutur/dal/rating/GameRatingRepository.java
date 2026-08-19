package be.technifutur.dal.rating;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRatingRepository extends JpaRepository<GameRatingEntity, Long> {

    List<GameRatingEntity> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<GameRatingEntity> findByUserIdAndIgdbGameId(Long userId, Long igdbGameId);

    @Query("select r.igdbGameId from GameRatingEntity r where r.userId = :userId")
    List<Long> findIgdbGameIdsByUserId(@Param("userId") Long userId);
}
