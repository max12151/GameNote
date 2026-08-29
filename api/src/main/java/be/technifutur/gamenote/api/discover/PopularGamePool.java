package be.technifutur.gamenote.api.discover;

import be.technifutur.gamenote.api.igdb.IgdbGameClient;
import be.technifutur.gamenote.api.igdb.IgdbGameResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Vivier des jeux les plus populaires, dans lequel la page Découvrir pioche au hasard.
 * <p>
 * Auparavant, chaque lot demandé par un utilisateur déclenchait un appel IGDB paginé et
 * trié par popularité : tout le monde voyait donc les mêmes jeux, dans le même ordre.
 * Ici, un vivier de {@value #POOL_SIZE} jeux est chargé une fois par genre, gardé
 * {@value #TTL_MINUTES} minutes et partagé par tous les utilisateurs ; le tirage aléatoire
 * se fait ensuite en mémoire. Le classement IGDB bouge de toute façon très lentement, donc
 * une donnée un peu tiède ne coûte rien, et les appels réseau chutent radicalement.
 */
@Component
public class PopularGamePool {

    private static final int POOL_SIZE = 200;
    private static final long TTL_MINUTES = 60;

    private final IgdbGameClient igdbGameClient;

    private final Map<String, CachedPool> pools = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    public PopularGamePool(IgdbGameClient igdbGameClient) {
        this.igdbGameClient = igdbGameClient;
    }

    /**
     * Tire au hasard jusqu'à {@code limit} jeux du vivier, en écartant ceux que
     * l'utilisateur a déjà vus.
     */
    public List<IgdbGameResult> pickRandom(String genre, Set<Long> excludedIgdbGameIds, int limit) {
        List<IgdbGameResult> candidates = new ArrayList<>(POOL_SIZE);

        for (IgdbGameResult game : getPool(genre)) {
            if (game.id() != null && !excludedIgdbGameIds.contains(game.id())) {
                candidates.add(game);
            }
        }

        // Mélanger puis découper coûte moins cher qu'un tri, et garantit qu'un même
        // utilisateur ne retombe pas deux fois de suite sur la même sélection.
        Collections.shuffle(candidates, ThreadLocalRandom.current());

        return candidates.size() <= limit ? List.copyOf(candidates) : List.copyOf(candidates.subList(0, limit));
    }

    private List<IgdbGameResult> getPool(String genre) {
        String key = normalizeGenre(genre);

        CachedPool cached = pools.get(key);
        if (cached != null && cached.isFresh()) {
            return cached.games();
        }

        // Verrou par genre : sans lui, N utilisateurs arrivant sur un cache froid
        // déclencheraient N appels IGDB identiques. Les autres genres restent servis
        // pendant ce temps.
        synchronized (locks.computeIfAbsent(key, unused -> new Object())) {
            cached = pools.get(key);
            if (cached != null && cached.isFresh()) {
                return cached.games();
            }

            List<IgdbGameResult> games = List.copyOf(igdbGameClient.fetchMostPopular(genre, POOL_SIZE));
            pools.put(key, new CachedPool(games, Instant.now().plus(Duration.ofMinutes(TTL_MINUTES))));

            return games;
        }
    }

    private String normalizeGenre(String genre) {
        return genre == null || genre.isBlank() ? "" : genre.strip().toLowerCase(Locale.ROOT);
    }

    private record CachedPool(List<IgdbGameResult> games, Instant expiresAt) {

        boolean isFresh() {
            return Instant.now().isBefore(expiresAt);
        }
    }
}
