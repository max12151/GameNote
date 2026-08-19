package be.technifutur.gamenote.api.rating;

import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingStatsDto;
import be.technifutur.il.rating.RatingFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ratings")
public class RatingController {

    private final RatingFacade ratingFacade;

    public RatingController(RatingFacade ratingFacade) {
        this.ratingFacade = ratingFacade;
    }

    @PostMapping
    public ResponseEntity<GameRatingDto> rateGame(Authentication authentication,
                                                    @Valid @RequestBody RateGameRequestDto request) {
        return ResponseEntity.ok(ratingFacade.rateGame(authentication.getName(), request));
    }

    @GetMapping
    public ResponseEntity<List<GameRatingDto>> getCollection(Authentication authentication) {
        return ResponseEntity.ok(ratingFacade.getCollection(authentication.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<RatingStatsDto> getStats(Authentication authentication) {
        return ResponseEntity.ok(ratingFacade.getStats(authentication.getName()));
    }

    @DeleteMapping("/{igdbGameId}")
    public ResponseEntity<Void> removeRating(Authentication authentication, @PathVariable Long igdbGameId) {
        ratingFacade.removeRating(authentication.getName(), igdbGameId);
        return ResponseEntity.noContent().build();
    }
}
