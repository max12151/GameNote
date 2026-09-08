package be.technifutur.bll.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.dal.follow.UserFollowRepository;
import be.technifutur.dal.list.GameListItemRepository;
import be.technifutur.dal.list.GameListRepository;
import be.technifutur.dal.reaction.CommentReactionRepository;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRepository;
import be.technifutur.dal.user.UserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Les opérations d'un membre sur son propre compte.
 * <p>
 * Toutes redemandent le mot de passe, et la suppression est irréversible : ce sont
 * exactement les endroits où une erreur ne se rattrape pas.
 */
class AccountServiceTest {

    private static final String PASSWORD = "MotDePasse123!";

    private UserRepository userRepository;
    private UserFollowRepository followRepository;
    private CommentReactionRepository reactionRepository;
    private GameListRepository listRepository;
    private GameListItemRepository listItemRepository;
    private PasswordEncoder passwordEncoder;
    private AccountService service;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        followRepository = mock(UserFollowRepository.class);
        reactionRepository = mock(CommentReactionRepository.class);
        listRepository = mock(GameListRepository.class);
        listItemRepository = mock(GameListItemRepository.class);
        // Un vrai encodeur, pas un mock : c'est la comparaison des empreintes qu'on veut
        // vérifier, et la simuler reviendrait à tester le simulacre.
        passwordEncoder = new BCryptPasswordEncoder();

        service = new AccountService(userRepository, followRepository, reactionRepository,
                listRepository, listItemRepository, passwordEncoder);

        user = new UserEntity();
        user.setId(42L);
        user.setUsername("Alice");
        user.setEmail("alice@example.com");
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setAvatarUrl("data:image/png;base64,AAAA");
        user.setBio("Joueuse de metroidvanias");
        user.setRole(UserRole.ADMIN);
        user.setCreatedAt(OffsetDateTime.now());

        when(userRepository.findByUsername("Alice")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(call -> call.getArgument(0));
        when(listRepository.findIdsByUserId(42L)).thenReturn(List.of());
    }

    @Test
    @DisplayName("Un mot de passe courant faux bloque le changement")
    void refusesWrongCurrentPassword() {
        assertThrows(InvalidCredentialsException.class,
                () -> service.changePassword("Alice", "pas le bon", "NouveauMdp123!"));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Le nouveau mot de passe remplace l'ancien")
    void changesPassword() {
        service.changePassword("Alice", PASSWORD, "NouveauMdp123!");

        org.junit.jupiter.api.Assertions.assertTrue(
                passwordEncoder.matches("NouveauMdp123!", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Reprendre le mot de passe déjà en place est refusé plutôt qu'accepté sans effet")
    void refusesIdenticalPassword() {
        assertThrows(InvalidOperationException.class,
                () -> service.changePassword("Alice", PASSWORD, PASSWORD));
    }

    @Test
    @DisplayName("Une adresse déjà prise par quelqu'un d'autre est refusée")
    void refusesTakenEmail() {
        when(userRepository.existsByEmailAndIdNot("prise@example.com", 42L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.changeEmail("Alice", PASSWORD, "prise@example.com"));
    }

    @Test
    @DisplayName("Reprendre sa propre adresse n'est pas un doublon")
    void allowsOwnEmail() {
        when(userRepository.existsByEmailAndIdNot(anyString(), anyLong())).thenReturn(false);

        assertEquals("alice@example.com", service.changeEmail("Alice", PASSWORD, " alice@example.com ").getEmail());
    }

    @Test
    @DisplayName("La suppression vide l'identité mais garde la ligne, qui porte notes et avis")
    void anonymizesInsteadOfDeleting() {
        service.deleteAccount("Alice", PASSWORD);

        assertEquals("Compte supprimé #42", user.getUsername());
        assertEquals("supprime-42@gamenote.invalid", user.getEmail());
        assertNull(user.getAvatarUrl());
        assertNull(user.getBio());
        assertNotNull(user.getDeletedAt());
        // Le rôle retombe à USER : un administrateur qui s'en va ne doit pas laisser derrière
        // lui une coquille qui compte encore parmi les comptes privilégiés.
        assertEquals(UserRole.USER, user.getRole());

        verify(userRepository, never()).delete(any());
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("La suppression rend l'empreinte du mot de passe inutilisable")
    void anonymizationClosesTheDoor() {
        String before = user.getPasswordHash();

        service.deleteAccount("Alice", PASSWORD);

        assertNotEquals(before, user.getPasswordHash());
        org.junit.jupiter.api.Assertions.assertFalse(passwordEncoder.matches(PASSWORD, user.getPasswordHash()));
    }

    @Test
    @DisplayName("La suppression coupe les liens sociaux et les réactions données")
    void anonymizationCutsSocialTies() {
        service.deleteAccount("Alice", PASSWORD);

        verify(followRepository).deleteAllInvolving(42L);
        verify(reactionRepository).deleteByUserId(42L);
    }

    @Test
    @DisplayName("Les listes du membre partent avec leur contenu")
    void anonymizationRemovesLists() {
        when(listRepository.findIdsByUserId(42L)).thenReturn(List.of(1L, 2L));

        service.deleteAccount("Alice", PASSWORD);

        verify(listItemRepository).deleteByGameListIds(List.of(1L, 2L));
        verify(listRepository).deleteByUserId(42L);
    }

    @Test
    @DisplayName("Un compte déjà supprimé ne se supprime pas deux fois")
    void refusesAlreadyDeletedAccount() {
        user.setDeletedAt(OffsetDateTime.now());

        assertThrows(InvalidOperationException.class, () -> service.deleteAccount("Alice", PASSWORD));
    }
}
