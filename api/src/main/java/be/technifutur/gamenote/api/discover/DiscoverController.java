package be.technifutur.gamenote.api.discover;

import be.technifutur.gamenote.api.igdb.IgdbGameClient;
import be.technifutur.gamenote.api.igdb.IgdbGameDto;
import be.technifutur.il.rating.RatingFacade;
import java.util.List;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discover")
public class DiscoverController {

    private final IgdbGameClient igdbGameClient;
    private final RatingFacade ratingFacade;

    public DiscoverController(IgdbGameClient igdbGameClient, RatingFacade ratingFacade) {
        this.igdbGameClient = igdbGameClient;
        this.ratingFacade = ratingFacade;
    }

    @GetMapping("/games")
    public List<IgdbGameDto> discoverGames(
            Authentication authentication,
            @RequestParam(required = false) String genre,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        Set<Long> alreadyRated = ratingFacade.getRatedIgdbGameIds(authentication.getName());

        return igdbGameClient.discoverGames(genre, alreadyRated, limit, offset).stream()
                .map(IgdbGameDto::from)
                .toList();
    }
}
