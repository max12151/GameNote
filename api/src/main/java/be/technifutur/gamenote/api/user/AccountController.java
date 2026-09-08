package be.technifutur.gamenote.api.user;

import be.technifutur.dl.user.ChangeEmailRequestDto;
import be.technifutur.dl.user.ChangePasswordRequestDto;
import be.technifutur.dl.user.DeleteAccountRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.user.AccountFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le compte, par opposition au profil : mot de passe, adresse e-mail, départ.
 * <p>
 * Contrôleur distinct de {@link UserController} — qui sert la biographie et l'avatar — parce
 * que ces trois opérations demandent toutes le mot de passe courant, et que les mélanger avec
 * la modification du profil finirait par affaiblir l'une ou alourdir l'autre.
 * <p>
 * La suppression passe par un {@code PUT} et non un {@code DELETE} : il faut un corps de
 * requête pour transporter le mot de passe, et un {@code DELETE} avec corps n'est pas
 * transmis de manière fiable par tous les intermédiaires HTTP.
 */
@RestController
@RequestMapping("/api/users/me")
public class AccountController {

    private final AccountFacade accountFacade;

    public AccountController(AccountFacade accountFacade) {
        this.accountFacade = accountFacade;
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication,
                                               @Valid @RequestBody ChangePasswordRequestDto request) {
        accountFacade.changePassword(authentication.getName(), request);

        // 204 et non le profil : rien n'a changé de ce que le front affiche, et renvoyer un
        // corps donnerait à croire qu'il faut le relire.
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/email")
    public ResponseEntity<UserDto> changeEmail(Authentication authentication,
                                               @Valid @RequestBody ChangeEmailRequestDto request) {
        return ResponseEntity.ok(accountFacade.changeEmail(authentication.getName(), request));
    }

    /**
     * Supprime le compte par anonymisation : le pseudo, l'adresse, l'avatar et la biographie
     * disparaissent, les notes et les avis restent, signés « Compte supprimé ».
     * <p>
     * Le jeton de session reste techniquement valide jusqu'à son expiration, mais le filtre
     * d'authentification refuse désormais ce compte : la session est close dès la requête
     * suivante, sans attendre.
     */
    @PutMapping("/deletion")
    public ResponseEntity<Void> deleteAccount(Authentication authentication,
                                              @Valid @RequestBody DeleteAccountRequestDto request) {
        accountFacade.deleteAccount(authentication.getName(), request);

        return ResponseEntity.noContent().build();
    }
}
