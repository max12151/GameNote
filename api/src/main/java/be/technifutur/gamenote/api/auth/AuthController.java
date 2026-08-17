package be.technifutur.gamenote.api.auth;

import be.technifutur.dl.auth.AuthResponseDto;
import be.technifutur.dl.auth.LoginRequestDto;
import be.technifutur.dl.auth.RegisterRequestDto;
import be.technifutur.il.auth.AuthFacade;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthFacade authFacade;

    public AuthController(AuthFacade authFacade) {
        this.authFacade = authFacade;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authFacade.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authFacade.login(request);
        return ResponseEntity.ok(response);
    }
}