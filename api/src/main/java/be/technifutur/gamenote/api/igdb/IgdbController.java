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

    public IgdbController(IgdbGameClient igdbGameClient) {
        this.igdbGameClient = igdbGameClient;
    }

    @GetMapping("/games")
    public List<IgdbGameResult> searchGames(
            @RequestParam String search,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return igdbGameClient.searchGames(search, limit);
    }
}