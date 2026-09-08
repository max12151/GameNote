package be.technifutur.gamenote.api.discover;

import be.technifutur.dl.discover.SkipGameRequestDto;
import be.technifutur.gamenote.api.igdb.IgdbGameDto;
import be.technifutur.il.discover.DiscoverFacade;
import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private static final int MAX_LIMIT = 50;

    private final PopularGamePool popularGamePool;
    private final DiscoverFacade discoverFacade;

    public DiscoverController(PopularGamePool popularGamePool, DiscoverFacade discoverFacade) {
        this.popularGamePool = popularGamePool;
        this.discoverFacade = discoverFacade;
    }

    /**
     * Sélection aléatoire parmi les jeux les plus populaires.
     *
     * @param exclude jeux déjà en file d'attente côté client : ils ne sont ni notés ni passés
     *                en base, le serveur ne peut donc pas deviner seul qu'ils sont "en cours"
     */
    @GetMapping("/games")
    public List<IgdbGameDto> discoverGames(
            Authentication authentication,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) List<Long> exclude
    ) {
        Set<Long> excluded = new HashSet<>(discoverFacade.getExcludedGameIds(authentication.getName()));

        if (exclude != null) {
            excluded.addAll(exclude);
        }

        return popularGamePool.pickRandom(genre, excluded, clampLimit(limit)).stream()
                .map(IgdbGameDto::from)
                .toList();
    }

    /** Écarte un jeu des suggestions de cet utilisateur pour la durée du délai configuré. */
    @PostMapping("/skip")
    public ResponseEntity<Void> skipGame(Authentication authentication,
                                         @Valid @RequestBody SkipGameRequestDto request) {
        discoverFacade.skipGame(authentication.getName(), request.getIgdbGameId());
        return ResponseEntity.noContent().build();
    }

    private static int clampLimit(int limit) {
        return Math.clamp(limit, 1, MAX_LIMIT);
    }
}
