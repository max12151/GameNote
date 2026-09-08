package be.technifutur.bll.user;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.follow.UserFollowRepository;
import be.technifutur.dal.list.GameListItemRepository;
import be.technifutur.dal.list.GameListRepository;
import be.technifutur.dal.reaction.CommentReactionRepository;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ce qu'un membre peut faire à son propre compte : changer son mot de passe, changer son
 * adresse, s'en aller.
 * <p>
 * Séparé de {@link UserService}, qui lit et écrit le profil visible ; ici on touche à
 * l'identité et aux accès, et chaque opération redemande le mot de passe.
 */
@Service
public class AccountService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final CommentReactionRepository commentReactionRepository;
    private final GameListRepository gameListRepository;
    private final GameListItemRepository gameListItemRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository,
                          UserFollowRepository userFollowRepository,
                          CommentReactionRepository commentReactionRepository,
                          GameListRepository gameListRepository,
                          GameListItemRepository gameListItemRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.commentReactionRepository = commentReactionRepository;
        this.gameListRepository = gameListRepository;
        this.gameListItemRepository = gameListItemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Change le mot de passe après vérification de l'ancien.
     * <p>
     * Redemander le mot de passe courant n'est pas une formalité : sans cela, quelqu'un
     * passant devant un écran resté ouvert changerait le mot de passe et prendrait le compte.
     */
    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        UserEntity user = requireActiveUser(username);

        requirePassword(user, currentPassword);

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new InvalidOperationException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * Change l'adresse e-mail, après vérification du mot de passe.
     * <p>
     * Aucun courriel de confirmation n'est envoyé : le site n'a pas d'infrastructure de mail,
     * et prétendre le contraire serait pire que de l'assumer. L'adresse ne sert donc qu'à
     * identifier le compte, jamais à prouver qu'on le contrôle.
     */
    @Transactional
    public UserEntity changeEmail(String username, String currentPassword, String newEmail) {
        UserEntity user = requireActiveUser(username);

        requirePassword(user, currentPassword);

        String email = newEmail.strip();

        // Réutiliser sa propre adresse n'est pas un doublon : c'est une opération sans effet,
        // qu'il serait absurde de refuser avec « cet email est déjà utilisé ».
        if (userRepository.existsByEmailAndIdNot(email, user.getId())) {
            throw new DuplicateResourceException("Cet email est déjà utilisé");
        }

        user.setEmail(email);
        return userRepository.save(user);
    }

    /**
     * Supprime le compte par anonymisation.
     * <p>
     * La ligne reste, vidée de tout ce qui désigne une personne : pseudo remplacé par un
     * « Compte supprimé » numéroté, adresse rendue inutilisable, empreinte de mot de passe
     * remplacée par une valeur aléatoire que rien ne peut deviner, avatar et biographie
     * effacés. Les notes et les avis, eux, demeurent : ce sont des données de la communauté,
     * les effacer ferait bouger le classement du site et laisserait des fils de discussion
     * troués. C'est le choix classique du « droit à l'oubli » appliqué à un site d'avis.
     * <p>
     * Disparaissent en revanche tout ce qui n'a de sens que rattaché à quelqu'un : les liens
     * de suivi dans les deux sens, les listes personnelles et les réactions données.
     */
    @Transactional
    public void deleteAccount(String username, String currentPassword) {
        UserEntity user = requireActiveUser(username);

        requirePassword(user, currentPassword);

        // Les éléments avant les listes : la base sait cascader, mais l'ordre explicite vaut
        // mieux que de dépendre d'une contrainte pour un enchaînement qu'on maîtrise.
        List<Long> listIds = gameListRepository.findIdsByUserId(user.getId());
        if (!listIds.isEmpty()) {
            gameListItemRepository.deleteByGameListIds(listIds);
            gameListRepository.deleteByUserId(user.getId());
        }

        userFollowRepository.deleteAllInvolving(user.getId());
        commentReactionRepository.deleteByUserId(user.getId());

        anonymize(user);
    }

    /**
     * Vide l'identité du compte tout en gardant sa ligne, sur laquelle pointent les notes et
     * les avis conservés.
     * <p>
     * Le pseudo reste unique — la contrainte de la base l'exige — mais devient lisible tel
     * quel à l'écran : « Compte supprimé #42 ». L'empreinte de mot de passe est celle d'une
     * valeur aléatoire jetée aussitôt : personne, pas même le titulaire, ne peut plus ouvrir
     * de session sur ce compte.
     */
    private void anonymize(UserEntity user) {
        user.setUsername("Compte supprimé #" + user.getId());
        user.setEmail("supprime-" + user.getId() + "@gamenote.invalid");
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setAvatarUrl(null);
        user.setBio(null);
        user.setRole(UserRole.USER);
        user.setSuspendedAt(null);
        user.setSuspensionReason(null);
        user.setDeletedAt(OffsetDateTime.now());

        userRepository.save(user);
    }

    private UserEntity requireActiveUser(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        if (!user.isActive()) {
            throw new InvalidOperationException("Ce compte n'est plus actif");
        }

        return user;
    }

    private void requirePassword(UserEntity user, String rawPassword) {
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException("Mot de passe incorrect");
        }
    }
}
