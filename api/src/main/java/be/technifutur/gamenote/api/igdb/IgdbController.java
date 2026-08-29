package be.technifutur.gamenote.api.igdb;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/igdb")
public class IgdbController {

    private final IgdbGameClient igdbGameClient;
    private final UpcomingGamePool upcomingGamePool;

    public IgdbController(IgdbGameClient igdbGameClient, UpcomingGamePool upcomingGamePool) {
        this.igdbGameClient = igdbGameClient;
        this.upcomingGamePool = upcomingGamePool;
    }

    @GetMapping("/games")
    public List<IgdbGameDto> searchGames(
            @RequestParam String search,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return igdbGameClient.searchGames(search, limit).stream()
                .map(IgdbGameDto::from)
                .toList();
    }

    /** Jeux pas encore sortis, du plus attendu au moins attendu. */
    @GetMapping("/upcoming")
    public List<IgdbGameDto> upcomingGames(
            @RequestParam(defaultValue = "12") int limit
    ) {
        return upcomingGamePool.getMostAnticipated(limit).stream()
                .map(IgdbGameDto::from)
                .toList();
    }
}