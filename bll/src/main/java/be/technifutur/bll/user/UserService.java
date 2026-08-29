package be.technifutur.bll.user;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
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
        user.setRole(UserRole.USER);
        user.setCreatedAt(OffsetDateTime.now());

        return userRepository.save(user);
    }

    /**
     * Change le rôle d'un compte. Renvoie {@code false} si le compte n'existe pas ou porte
     * déjà ce rôle, pour que l'appelant sache s'il y a réellement eu une promotion.
     */
    @Transactional
    public boolean setRole(String username, UserRole role) {
        UserEntity user = userRepository.findByUsername(username).orElse(null);

        if (user == null || user.getRole() == role) {
            return false;
        }

        user.setRole(role);
        userRepository.save(user);
        return true;
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

    /**
     * Version de {@link #findByUsername(String)} pour les appelants qui travaillent déjà
     * sur un utilisateur authentifié : son absence en base est une anomalie, pas un cas
     * fonctionnel à traiter à chaque appel.
     */
    public UserEntity getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));
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
