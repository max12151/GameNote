package be.technifutur.bll.user;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity register(String username,
                               String email,
                               String rawPassword,
                               String avatarUrl,
                               String bio) {

        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Ce nom d'utilisateur est déjà utilisé");
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Cet email est déjà utilisé");
        }

        UserEntity user = new UserEntity();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setAvatarUrl(avatarUrl);
        user.setBio(bio);
        user.setCreatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    @Transactional
    public UserEntity authenticate(String username, String rawPassword) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        return user;
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public UserEntity updateProfile(String username, String bio, String avatarUrl) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setBio(bio);
        user.setAvatarUrl(avatarUrl);

        return userRepository.save(user);
    }
}
