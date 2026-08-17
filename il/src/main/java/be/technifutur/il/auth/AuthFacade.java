package be.technifutur.il.auth;

import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.auth.AuthResponseDto;
import be.technifutur.dl.auth.LoginRequestDto;
import be.technifutur.dl.auth.RegisterRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.user.UserMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {

    private final UserService userService;
    private final UserMapper userMapper;

    public AuthFacade(UserService userService,
                      UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    public AuthResponseDto register(RegisterRequestDto request) {
        UserEntity user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getAvatarUrl(),
                request.getBio()
        );
        UserDto userDto = userMapper.toDto(user);

        String token = generateTokenFor(user); // à remplacer plus tard par un vrai JWT

        return new AuthResponseDto(token, userDto);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        UserEntity user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!userService.verifyPassword(user, request.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        UserDto userDto = userMapper.toDto(user);
        String token = generateTokenFor(user);

        return new AuthResponseDto(token, userDto);
    }

    private String generateTokenFor(UserEntity user) {
        // Pour l'instant: token simple, plus tard JWT / session
        return UUID.randomUUID().toString();
    }
}