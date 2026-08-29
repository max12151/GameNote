package be.technifutur.il.user;

import be.technifutur.bll.user.AvatarImage;
import be.technifutur.bll.user.AvatarService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.user.UpdateProfileRequestDto;
import be.technifutur.dl.user.UserDto;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ProfileFacade {

    private final UserService userService;
    private final AvatarService avatarService;
    private final UserMapper userMapper;

    public ProfileFacade(UserService userService, AvatarService avatarService, UserMapper userMapper) {
        this.userService = userService;
        this.avatarService = avatarService;
        this.userMapper = userMapper;
    }

    public Optional<AvatarImage> findAvatar(Long userId) {
        return avatarService.findAvatar(userId);
    }

    public UserDto getProfile(String username) {
        return userMapper.toDto(userService.getByUsername(username));
    }

    public UserDto updateProfile(String username, UpdateProfileRequestDto request) {
        UserEntity user = userService.updateProfile(username, request.getBio(), request.getAvatarUrl());
        return userMapper.toDto(user);
    }
}
