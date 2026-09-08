package be.technifutur.gamenote.api.igdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Traduction d'une réponse IGDB en objet exposé au front.
 * <p>
 * Ce test remplace un ancien {@code IgdbUrlTest} qui démarrait tout le contexte Spring
 * pour vérifier qu'une constante de configuration valait bien ce qui était écrit dans le
 * fichier de configuration — il exigeait donc une base et des identifiants Twitch pour ne
 * rien affirmer d'utile. Ce qui mérite d'être tenu, c'est la conversion elle-même : elle
 * décide de ce que le navigateur reçoit, et elle est exécutable partout.
 */
class IgdbGameDtoTest {

    @Test
    @DisplayName("Les adresses sans protocole reçoivent https, et la jaquette passe en grand format")
    void normalisesUrls() {
        IgdbGameResult result = gameWith(
                new IgdbGameResult.Cover("//images.igdb.com/igdb/image/upload/t_thumb/abc.jpg"),
                List.of(new IgdbGameResult.Artwork("//images.igdb.com/igdb/image/upload/t_thumb/art.jpg")),
                null);

        IgdbGameDto dto = IgdbGameDto.from(result);

        assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_big/abc.jpg", dto.coverUrl());
        assertEquals(List.of("https://images.igdb.com/igdb/image/upload/t_thumb/art.jpg"), dto.artworkUrls());
    }

    @Test
    @DisplayName("Une adresse déjà complète est laissée telle quelle")
    void keepsAbsoluteUrls() {
        IgdbGameResult result = gameWith(
                new IgdbGameResult.Cover("https://cdn.example/t_cover_big/x.jpg"), null, null);

        assertEquals("https://cdn.example/t_cover_big/x.jpg", IgdbGameDto.from(result).coverUrl());
    }

    @Test
    @DisplayName("Les listes absentes deviennent vides, jamais nulles")
    void neverReturnsNullLists() {
        IgdbGameDto dto = IgdbGameDto.from(gameWith(null, null, null));

        assertTrue(dto.genres().isEmpty());
        assertTrue(dto.platforms().isEmpty());
        assertTrue(dto.artworkUrls().isEmpty());
        assertTrue(dto.screenshotUrls().isEmpty());
        assertTrue(dto.developers().isEmpty());
        assertTrue(dto.publishers().isEmpty());
    }

    @Test
    @DisplayName("Un studio est rangé selon son rôle, et peut tenir les deux")
    void splitsDevelopersAndPublishers() {
        List<IgdbGameResult.InvolvedCompany> companies = List.of(
                involved("FromSoftware", true, false),
                involved("Bandai Namco", false, true),
                involved("Nintendo", true, true));

        IgdbGameDto dto = IgdbGameDto.from(gameWith(null, null, companies));

        assertEquals(List.of("FromSoftware", "Nintendo"), dto.developers());
        assertEquals(List.of("Bandai Namco", "Nintendo"), dto.publishers());
    }

    @Test
    @DisplayName("Un studio sans nom est écarté plutôt que remonté en null")
    void dropsNamelessCompanies() {
        List<IgdbGameResult.InvolvedCompany> companies = Arrays.asList(
                new IgdbGameResult.InvolvedCompany(null, true, false),
                involved("Capcom", true, false));

        assertEquals(List.of("Capcom"), IgdbGameDto.from(gameWith(null, null, companies)).developers());
    }

    private static IgdbGameResult.InvolvedCompany involved(String name, boolean developer, boolean publisher) {
        return new IgdbGameResult.InvolvedCompany(new IgdbGameResult.Company(1L, name), developer, publisher);
    }

    private static IgdbGameResult gameWith(IgdbGameResult.Cover cover,
                                           List<IgdbGameResult.Artwork> artworks,
                                           List<IgdbGameResult.InvolvedCompany> companies) {
        return new IgdbGameResult(
                1L, "Un jeu", "Un résumé", 0L, 1_700_000_000L,
                cover, null, null, null, null, null, null,
                companies, artworks, null);
    }
}
