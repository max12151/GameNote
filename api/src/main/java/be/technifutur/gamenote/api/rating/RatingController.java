package be.technifutur.gamenote.api.rating;

import be.technifutur.dl.rating.GameRatingDto;
import be.technifutur.dl.rating.GameStatusDto;
import be.technifutur.dl.rating.LibrarySummaryDto;
import be.technifutur.dl.rating.RateGameRequestDto;
import be.technifutur.dl.rating.RatingStatsDto;
import be.technifutur.dl.rating.SetStatusRequestDto;
import be.technifutur.il.rating.RatingFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * La bibliothèque d'un joueur.
 * <p>
 * Le chemin reste {@code /api/ratings} bien que la ressource ne soit plus seulement une note :
 * depuis les statuts, une entrée peut exister sans note — une envie, un jeu en cours. Renommer
 * le chemin aurait cassé tous les liens et tous les appels existants pour un gain de vocabulaire.
 */
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

    /**
     * Range un jeu sous un statut, sans le noter : « à jouer », « en cours », « terminé »,
     * « abandonné ».
     * <p>
     * Crée l'entrée si le jeu n'était pas dans la bibliothèque — c'est le geste « ajouter à ma
     * liste d'envies » depuis la recherche — et conserve la note s'il y en avait une.
     */
    @PutMapping("/status")
    public ResponseEntity<GameRatingDto> setStatus(Authentication authentication,
                                                   @Valid @RequestBody SetStatusRequestDto request) {
        return ResponseEntity.ok(ratingFacade.setStatus(authentication.getName(), request));
    }

    /**
     * La bibliothèque, entière ou restreinte à un onglet.
     *
     * @param status statut demandé, ou absent pour tout voir
     */
    @GetMapping
    public ResponseEntity<List<GameRatingDto>> getCollection(Authentication authentication,
                                                             @RequestParam(required = false) GameStatusDto status) {
        return ResponseEntity.ok(ratingFacade.getCollection(authentication.getName(), status));
    }

    /** Les compteurs des quatre onglets, pour les afficher sans charger quatre fois la liste. */
    @GetMapping("/library")
    public ResponseEntity<LibrarySummaryDto> getLibrarySummary(Authentication authentication) {
        return ResponseEntity.ok(ratingFacade.getLibrarySummary(authentication.getName()));
    }

    @GetMapping("/stats")
    public ResponseEntity<RatingStatsDto> getStats(Authentication authentication) {
        return ResponseEntity.ok(ratingFacade.getStats(authentication.getName()));
    }

    /** Retire complètement le jeu de la bibliothèque : la note, le statut et l'avis partent ensemble. */
    @DeleteMapping("/{igdbGameId}")
    public ResponseEntity<Void> removeRating(Authentication authentication, @PathVariable Long igdbGameId) {
        ratingFacade.removeRating(authentication.getName(), igdbGameId);
        return ResponseEntity.noContent().build();
    }
}
