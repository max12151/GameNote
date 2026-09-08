package be.technifutur.bll.user;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.technifutur.dal.user.UserRepository;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Décodage des avatars stockés en data URI.
 * <p>
 * La chaîne décodée vient d'un utilisateur et son type MIME finit dans un en-tête
 * {@code Content-Type} : la liste blanche est ce qui empêche de faire servir du HTML
 * depuis le domaine de l'API. Un test la fige.
 */
class AvatarServiceTest {

    private static final byte[] PIXEL = {1, 2, 3, 4};

    private UserRepository repository;
    private AvatarService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserRepository.class);
        service = new AvatarService(repository);
    }

    @Test
    @DisplayName("Une data URI d'image valide est décodée telle quelle")
    void decodesImage() {
        AvatarImage avatar = findAvatar(dataUri("image/png", PIXEL)).orElseThrow();

        assertEquals("image/png", avatar.contentType());
        assertArrayEquals(PIXEL, avatar.data());
        assertTrue(avatar.etag().startsWith("\"") && avatar.etag().endsWith("\""));
    }

    @Test
    @DisplayName("Le type MIME est comparé sans tenir compte de la casse")
    void acceptsUppercaseContentType() {
        assertEquals("image/jpeg", findAvatar(dataUri("IMAGE/JPEG", PIXEL)).orElseThrow().contentType());
    }

    @Test
    @DisplayName("Deux contenus identiques donnent le même ETag, deux contenus différents non")
    void etagFollowsContent() {
        String first = findAvatar(dataUri("image/png", PIXEL)).orElseThrow().etag();
        String same = findAvatar(dataUri("image/png", PIXEL)).orElseThrow().etag();
        String other = findAvatar(dataUri("image/png", new byte[] {9, 9})).orElseThrow().etag();

        assertEquals(first, same);
        assertTrue(!first.equals(other));
    }

    @Test
    @DisplayName("Tout ce qui n'est pas une image de la liste blanche est refusé")
    void rejectsAnythingElse() {
        assertTrue(findAvatar(dataUri("text/html", "<script>".getBytes())).isEmpty());
        assertTrue(findAvatar(dataUri("image/svg+xml", "<svg/>".getBytes())).isEmpty());
        assertTrue(findAvatar("https://exemple.test/photo.png").isEmpty());
        assertTrue(findAvatar("data:image/png,pas-de-base64").isEmpty());
        assertTrue(findAvatar("data:image/png;base64,%%%").isEmpty());
        assertTrue(findAvatar("   ").isEmpty());
    }

    @Test
    @DisplayName("Un compte sans avatar n'a rien à servir")
    void handlesMissingAvatar() {
        when(repository.findAvatarUrlById(anyLong())).thenReturn(Optional.empty());

        assertTrue(service.findAvatar(1L).isEmpty());
    }

    private Optional<AvatarImage> findAvatar(String storedValue) {
        when(repository.findAvatarUrlById(anyLong())).thenReturn(Optional.of(storedValue));
        return service.findAvatar(1L);
    }

    private static String dataUri(String contentType, byte[] data) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(data);
    }
}
