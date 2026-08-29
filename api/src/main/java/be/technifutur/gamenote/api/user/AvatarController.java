package be.technifutur.gamenote.api.user;

import be.technifutur.bll.user.AvatarImage;
import be.technifutur.il.user.ProfileFacade;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;

/**
 * Sert les avatars comme de vraies images plutôt qu'en base64 dans le JSON.
 * <p>
 * La route est publique parce qu'une balise {@code <img src>} ne peut pas porter le jeton
 * JWT : la protéger obligerait le front à télécharger chaque image en JavaScript pour la
 * convertir en blob, ce qui supprimerait tout bénéfice du cache navigateur. Un avatar n'est
 * de toute façon pas une donnée sensible, et la route n'expose rien d'autre.
 */
@RestController
@RequestMapping("/api/users")
public class AvatarController {

    private static final Duration CACHE_DURATION = Duration.ofHours(1);

    private final ProfileFacade profileFacade;

    public AvatarController(ProfileFacade profileFacade) {
        this.profileFacade = profileFacade;
    }

    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId, WebRequest request) {
        AvatarImage avatar = profileFacade.findAvatar(userId).orElse(null);

        // 404 plutôt qu'une image par défaut : le front sait déjà, grâce au drapeau porté
        // par chaque commentaire, s'il doit afficher une image ou les initiales.
        if (avatar == null) {
            return ResponseEntity.notFound().build();
        }

        // Renvoie 304 si le navigateur a déjà cette version : rien ne repart sur le réseau.
        if (request.checkNotModified(avatar.etag())) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).eTag(avatar.etag()).build();
        }

        return ResponseEntity.ok()
                .eTag(avatar.etag())
                .cacheControl(CacheControl.maxAge(CACHE_DURATION).cachePublic())
                .contentType(MediaType.parseMediaType(avatar.contentType()))
                // Le type MIME vient d'une donnée utilisateur : même filtré par la liste
                // blanche du service, on interdit au navigateur de le redeviner.
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Disposition", "inline")
                .body(avatar.data());
    }
}
