package be.technifutur.bll.discover;

import be.technifutur.dal.discover.GameSkipEntity;
import be.technifutur.dal.discover.GameSkipRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère les jeux "passés" dans la page Découvrir : un jeu écarté n'est plus proposé à cet
 * utilisateur pendant {@code gamenote.discover.skip-cooldown-days} jours, puis redevient
 * éligible (l'envie de le découvrir peut avoir changé entre-temps).
 */
@Service
public class GameSkipService {

    private final GameSkipRepository gameSkipRepository;
    private final long cooldownDays;

    public GameSkipService(GameSkipRepository gameSkipRepository,
                           @Value("${gamenote.discover.skip-cooldown-days:7}") long cooldownDays) {
        this.gameSkipRepository = gameSkipRepository;
        this.cooldownDays = cooldownDays;
    }

    @Transactional
    public void skip(Long userId, Long igdbGameId) {
        OffsetDateTime now = OffsetDateTime.now();

        // Repasser un jeu déjà passé relance le délai plutôt que de créer un doublon
        // (la contrainte d'unicité en base le refuserait de toute façon).
        GameSkipEntity entity = gameSkipRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseGet(() -> {
                    GameSkipEntity created = new GameSkipEntity();
                    created.setUserId(userId);
                    created.setIgdbGameId(igdbGameId);
                    return created;
                });

        entity.setSkippedAt(now);
        gameSkipRepository.save(entity);

        // Purge opportuniste : les jeux dont le délai est écoulé n'ont plus aucune utilité.
        // La faire ici (plutôt que dans une tâche planifiée) garde la table petite sans
        // ajouter d'ordonnanceur, et ne coûte qu'un DELETE indexé sur les lignes de ce seul
        // utilisateur. La ligne qu'on vient d'écrire est postérieure au seuil, donc préservée.
        gameSkipRepository.deleteExpiredForUser(userId, now.minusDays(cooldownDays));
    }

    /** Identifiants des jeux encore sous le coup du délai de réapparition. */
    public List<Long> getActiveSkippedGameIds(Long userId) {
        return gameSkipRepository.findIgdbGameIdsSkippedSince(userId,
                OffsetDateTime.now().minusDays(cooldownDays));
    }
}
