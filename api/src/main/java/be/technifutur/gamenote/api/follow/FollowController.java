package be.technifutur.gamenote.api.follow;

import be.technifutur.dl.user.FollowStatsDto;
import be.technifutur.dl.user.PlayerListDto;
import be.technifutur.il.follow.FollowFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Suivre un joueur, et lire qui suit qui.
 * <p>
 * Les routes vivent sous {@code /api/users/{id}} : le suivi est une propriété du profil visé,
 * pas une ressource à part. Les deux opérations sont idempotentes et renvoient les compteurs
 * à jour, ce qui évite au front un second appel pour rafraîchir le bouton.
 */
@RestController
@RequestMapping("/api/users")
public class FollowController {

    private final FollowFacade followFacade;

    public FollowController(FollowFacade followFacade) {
        this.followFacade = followFacade;
    }

    @PostMapping("/{id}/follow")
    public ResponseEntity<FollowStatsDto> follow(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followFacade.follow(authentication.getName(), id));
    }

    @DeleteMapping("/{id}/follow")
    public ResponseEntity<FollowStatsDto> unfollow(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followFacade.unfollow(authentication.getName(), id));
    }

    /** Les joueurs que ce membre suit. */
    @GetMapping("/{id}/following")
    public ResponseEntity<PlayerListDto> getFollowing(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followFacade.getFollowing(authentication.getName(), id));
    }

    /** Les membres qui suivent ce joueur. */
    @GetMapping("/{id}/followers")
    public ResponseEntity<PlayerListDto> getFollowers(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(followFacade.getFollowers(authentication.getName(), id));
    }
}
