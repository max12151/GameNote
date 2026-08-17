package be.technifutur.gamenote.api.user;

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
}
