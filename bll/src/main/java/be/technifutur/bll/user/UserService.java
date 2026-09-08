package be.technifutur.bll.user;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.bll.security.AuthenticatedUser;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
import be.technifutur.dal.user.UserSearchView;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    /** En deçà de deux caractères, une recherche de pseudo ne rendrait que du bruit. */
    private static final int MIN_SEARCH_LENGTH = 2;

    /**
     * Plafond des résultats. Le nombre voulu vient de l'appelant, mais pas sans limite :
     * une valeur passée dans l'URL ne doit pas pouvoir demander la table entière.
     */
    private static final int MAX_SEARCH_RESULTS = 30;

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

        // Un compte anonymisé porte l'empreinte d'un mot de passe aléatoire jeté aussitôt :
        // en pratique on ne passe jamais la vérification ci-dessus. Le cas est traité malgré
        // tout, et avec le même message qu'un mot de passe faux — dire « ce compte a été
        // supprimé » renseignerait sur l'existence passée d'un pseudo.
        if (user.isDeleted()) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        // La suspension, elle, se dit : le titulaire a le droit de savoir pourquoi il ne
        // rentre plus, et le message est la seule voie pour le lui apprendre.
        if (user.isSuspended()) {
            throw new ForbiddenOperationException(suspensionMessage(user));
        }

        return user;
    }

    private static String suspensionMessage(UserEntity user) {
        String reason = user.getSuspensionReason();

        return reason == null || reason.isBlank()
                ? "Ce compte est suspendu."
                : "Ce compte est suspendu : " + reason;
    }

    /**
     * Membres dont le pseudo contient le terme cherché.
     * <p>
     * Un terme vide ou trop court rend une liste vide plutôt que tout le monde : chercher
     * « a » n'a pas de sens, et laisser passer la requête reviendrait à publier l'annuaire
     * des comptes du site à qui vide le champ.
     */
    public List<UserSearchView> search(String term, int limit) {
        String safeTerm = term == null ? "" : term.strip();

        if (safeTerm.length() < MIN_SEARCH_LENGTH) {
            return List.of();
        }

        return userRepository.searchByUsername(safeTerm,
                PageRequest.of(0, Math.clamp(limit, 1, MAX_SEARCH_RESULTS)));
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * Le porteur d'un jeton, en une projection légère : identifiant, pseudo, rôle et état du
     * compte.
     * <p>
     * Appelée à chaque requête entrante par le filtre d'authentification. Le rôle est lu en
     * base et non dans le jeton : un administrateur rétrogradé, ou un compte suspendu, doit
     * perdre ses droits tout de suite, et non à l'expiration d'un jeton valable un jour.
     */
    public Optional<AuthenticatedUser> findAuthenticated(String username) {
        return userRepository.findAuthByUsername(username)
                .map(view -> new AuthenticatedUser(
                        view.getId(),
                        view.getUsername(),
                        view.getRole() == null ? UserRole.USER : view.getRole(),
                        view.getSuspendedAt() == null && view.getDeletedAt() == null));
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

    /**
     * Lecture par identifiant, pour les pages qui parlent d'un joueur autre que celui qui
     * est connecté. Renvoie un {@link Optional} plutôt que de lever : l'identifiant vient
     * d'une URL, un profil supprimé ou inventé est un 404 ordinaire, pas une anomalie.
     */
    public Optional<UserEntity> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Variante employée par les pages publiques.
     * <p>
     * Un compte anonymisé n'a plus de profil à montrer ; un compte suspendu ne doit plus en
     * montrer. Les deux répondent 404, comme un identifiant inventé — dire « ce compte est
     * suspendu » publierait une décision de modération à qui tape une URL.
     * <p>
     * La console d'administration, elle, passe par {@code findById} : c'est justement son
     * rôle de voir ce que le site cache.
     */
    public Optional<UserEntity> findVisibleById(Long id) {
        return userRepository.findById(id).filter(UserEntity::isActive);
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
