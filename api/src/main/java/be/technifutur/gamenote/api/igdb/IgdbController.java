package be.technifutur.gamenote.api.igdb;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/igdb")
public class IgdbController {
    private final IgdbGameClient client;

    public IgdbController(IgdbGameClient client) { this.client = client; }

    @GetMapping("/games/search")
    public List<IgdbGameResult> search(@RequestParam String title,
                                       @RequestParam(defaultValue = "10") int limit) {
        try { return client.search(title, limit); }
        catch (IllegalStateException exception) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "IGDB is not configured", exception); }
        catch (RuntimeException exception) { throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IGDB request failed", exception); }
    }
}
