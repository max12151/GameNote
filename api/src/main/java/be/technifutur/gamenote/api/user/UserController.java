package be.technifutur.gamenote.api.user;

import be.technifutur.dl.user.PublicProfileDto;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.user.ProfileFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileFacade profileFacade;

    public UserController(ProfileFacade profileFacade) {
        this.profileFacade = profileFacade;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        return ResponseEntity.ok(profileFacade.getProfile(authentication.getName()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(Authentication authentication,
                                                       @Valid @RequestBody UpdateProfileRequestDto request) {
        return ResponseEntity.ok(profileFacade.updateProfile(authentication.getName(), request));
    }

    /**
     * Profil d'un joueur vu par un autre membre : statistiques de notation et derniers avis.
     * <p>
     * La réponse est un {@link PublicProfileDto}, un type qui ne possède pas de champ
     * e-mail — et non un {@code UserDto} dont on aurait effacé l'adresse avant l'envoi.
     * <p>
     * Route authentifiée : lire le classement du site ne demande pas de compte, mais
     * consulter la page de quelqu'un, si.
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<PublicProfileDto> getPublicProfile(@PathVariable Long id) {
        return profileFacade.getPublicProfile(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
