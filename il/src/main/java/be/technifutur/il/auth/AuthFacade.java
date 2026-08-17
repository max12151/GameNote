package be.technifutur.il.auth;

import be.technifutur.bll.security.JwtService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.auth.AuthResponseDto;
import be.technifutur.dl.auth.LoginRequestDto;
import be.technifutur.dl.auth.RegisterRequestDto;
import be.technifutur.dl.user.UserDto;
import be.technifutur.il.user.UserMapper;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {

    private final UserService userService;
    private final UserMapper userMapper;
    private final JwtService jwtService;

    public AuthFacade(UserService userService,
                      UserMapper userMapper,
                      JwtService jwtService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
    }

    public AuthResponseDto register(RegisterRequestDto request) {
        UserEntity user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getAvatarUrl(),
                request.getBio()
        );
        return toAuthResponse(user);
    }

    public AuthResponseDto login(LoginRequestDto request) {
        UserEntity user = userService.authenticate(request.getUsername(), request.getPassword());
        return toAuthResponse(user);
    }

    private AuthResponseDto toAuthResponse(UserEntity user) {
        UserDto userDto = userMapper.toDto(user);
        String token = jwtService.generateToken(user.getUsername());
        return new AuthResponseDto(token, userDto);
    }
}
