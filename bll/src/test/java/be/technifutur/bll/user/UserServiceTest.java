package be.technifutur.bll.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Ce qu'un compte suspendu ou anonymisé n'a plus le droit de faire, et ce qu'on n'a plus le
 * droit d'en montrer.
 * <p>
 * Ces deux états ne changent rien à ce qu'on voit à l'écran tant qu'aucun compte n'est dans
 * cet état : une régression y passerait inaperçue jusqu'au jour où une suspension ne
 * suspendrait plus rien.
 */
class UserServiceTest {

    private static final String PASSWORD = "MotDePasse123!";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        // Un vrai encodeur : c'est la comparaison des empreintes qu'on veut vérifier, et la
        // simuler reviendrait à tester le simulacre.
        passwordEncoder = new BCryptPasswordEncoder();
        service = new UserService(userRepository, passwordEncoder);

        user = new UserEntity();
        user.setId(7L);
        user.setUsername("Alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(UserRole.USER);
        user.setCreatedAt(OffsetDateTime.now());

        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("Un compte en règle ouvre bien une session")
    void authenticatesActiveAccount() {
        assertEquals(7L, service.authenticate("Alice", PASSWORD).getId());
    }

    @Test
    @DisplayName("Un compte suspendu ne se connecte plus, et le motif lui est dit")
    void refusesSuspendedAccount() {
        user.setSuspendedAt(OffsetDateTime.now());
        user.setSuspensionReason("Propos répétés hors sujet");

        ForbiddenOperationException refus = assertThrows(ForbiddenOperationException.class,
                () -> service.authenticate("Alice", PASSWORD));

        // Le titulaire a le droit de savoir pourquoi il ne rentre plus : le message est la
        // seule voie pour le lui apprendre.
        assertTrue(refus.getMessage().contains("Propos répétés hors sujet"));
    }

    @Test
    @DisplayName("Une suspension sans motif se dit quand même")
    void statesSuspensionWithoutReason() {
        user.setSuspendedAt(OffsetDateTime.now());

        assertThrows(ForbiddenOperationException.class, () -> service.authenticate("Alice", PASSWORD));
    }

    @Test
    @DisplayName("Un compte anonymisé est refusé comme un mot de passe faux")
    void hidesDeletedAccountBehindTheUsualMessage() {
        user.setDeletedAt(OffsetDateTime.now());

        InvalidCredentialsException refus = assertThrows(InvalidCredentialsException.class,
                () -> service.authenticate("Alice", PASSWORD));

        // Dire « ce compte a été supprimé » renseignerait sur l'existence passée d'un pseudo.
        assertEquals("Identifiants invalides", refus.getMessage());
    }

    @Test
    @DisplayName("Seul un compte en règle a une page publique")
    void hidesSuspendedAndDeletedFromPublicPages() {
        assertTrue(service.findVisibleById(7L).isPresent());

        user.setSuspendedAt(OffsetDateTime.now());
        assertTrue(service.findVisibleById(7L).isEmpty(), "un compte suspendu n'a plus de profil");

        user.setSuspendedAt(null);
        user.setDeletedAt(OffsetDateTime.now());
        assertTrue(service.findVisibleById(7L).isEmpty(), "un compte anonymisé n'a plus de profil");
    }

    @Test
    @DisplayName("La console, elle, continue de voir ces comptes")
    void keepsThemReachableForModeration() {
        user.setSuspendedAt(OffsetDateTime.now());

        // C'est le rôle de la console de voir ce que le site cache : sans cela, un compte
        // suspendu ne pourrait plus être réactivé.
        assertTrue(service.findById(7L).isPresent());
    }
}
