package be.technifutur.il.user;

import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import org.springframework.stereotype.Component;

@Component
public class ProfileFacade {

    private final UserService userService;
    private final UserMapper userMapper;

    public ProfileFacade(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    public UserDto getProfile(String username) {
        UserEntity user = userService.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
        return userMapper.toDto(user);
    }

    public UserDto updateProfile(String username, UpdateProfileRequestDto request) {
        UserEntity user = userService.updateProfile(username, request.getBio(), request.getAvatarUrl());
        return userMapper.toDto(user);
    }
}
