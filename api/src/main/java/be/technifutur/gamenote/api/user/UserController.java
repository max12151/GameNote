package be.technifutur.gamenote.api.user;

import be.technifutur.dl.comment.CommentPageDto;
import be.technifutur.dl.user.PublicProfileDto;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.dl.user.UserSearchResultDto;
import be.technifutur.il.user.ProfileFacade;
import jakarta.validation.Valid;
import java.util.List;
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
     * consulter la page de quelqu'un, si. L'identité de qui regarde sert aussi à l'état du
     * bouton « suivre » et au filtrage des listes privées.
     */
    @GetMapping("/{id}/profile")
    public ResponseEntity<PublicProfileDto> getPublicProfile(Authentication authentication,
                                                             @PathVariable Long id) {
        return profileFacade.getPublicProfile(authentication.getName(), id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Suite du fil d'avis d'un joueur, dont le profil a déjà servi la première page.
     * <p>
     * Route séparée plutôt qu'un profil qui grossirait : recharger la fiche entière —
     * statistiques, répartition par genre, coup de cœur — pour dix avis de plus ferait
     * repayer à chaque clic ce qui n'a pas bougé.
     */
    @GetMapping("/{id}/comments")
    public ResponseEntity<CommentPageDto> getPlayerComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return profileFacade.getPlayerComments(id, page, size)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Recherche de membres par pseudo.
     * <p>
     * Route authentifiée, comme les profils qu'elle mène à ouvrir : la liste des comptes
     * du site n'a pas à être feuilletable par un visiteur anonyme. Un terme trop court
     * rend une liste vide plutôt que tout le monde (cf. {@code UserService.search}).
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserSearchResultDto>> searchPlayers(
            Authentication authentication,
            @RequestParam(name = "q") String query,
            @RequestParam(defaultValue = "12") int limit
    ) {
        return ResponseEntity.ok(profileFacade.searchPlayers(authentication.getName(), query, limit));
    }
}
