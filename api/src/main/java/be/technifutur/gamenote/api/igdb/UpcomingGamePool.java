package be.technifutur.gamenote.api.igdb;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Cache des sorties à venir affichées sur l'accueil.
 * <p>
 * La page d'accueil est publique : sans cache, chaque visiteur déclencherait un appel IGDB.
 * Or le classement des jeux les plus attendus bouge à l'échelle de la semaine, pas de la
 * seconde — une donnée vieille de quelques heures est parfaitement acceptable ici.
 */
@Component
public class UpcomingGamePool {

    private static final int POOL_SIZE = 24;
    private static final Duration TTL = Duration.ofHours(6);

    private final IgdbGameClient igdbGameClient;
    private final Object lock = new Object();

    private volatile List<IgdbGameResult> games = List.of();
    private volatile Instant expiresAt = Instant.EPOCH;

    public UpcomingGamePool(IgdbGameClient igdbGameClient) {
        this.igdbGameClient = igdbGameClient;
    }

    public List<IgdbGameResult> getMostAnticipated(int limit) {
        List<IgdbGameResult> pool = getPool();
        int safeLimit = Math.min(Math.max(limit, 1), POOL_SIZE);

        return pool.size() <= safeLimit ? pool : List.copyOf(pool.subList(0, safeLimit));
    }

    private List<IgdbGameResult> getPool() {
        if (Instant.now().isBefore(expiresAt)) {
            return games;
        }

        // Verrou unique : le vivier n'a pas de variante (pas de genre), une seule reconstruction
        // à la fois suffit et évite que N visiteurs simultanés déclenchent N appels identiques.
        synchronized (lock) {
            if (Instant.now().isBefore(expiresAt)) {
                return games;
            }

            games = List.copyOf(igdbGameClient.fetchUpcoming(POOL_SIZE));
            expiresAt = Instant.now().plus(TTL);

            return games;
        }
    }
}
