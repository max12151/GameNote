package be.technifutur.bll.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Normalisation des filtres du classement.
 * <p>
 * Ces valeurs viennent d'une URL, donc de n'importe où. Une erreur ici ne lève pas
 * d'exception : elle produit silencieusement une page vide, ou pire, un classement filtré
 * autrement que ce que l'écran affiche.
 */
class RankingQueryTest {

    @Test
    @DisplayName("Un filtre vide ou fait d'espaces vaut absence de filtre")
    void blankFiltersBecomeNull() {
        RankingQuery query = new RankingQuery("  ", "   ", "", null, null, null);

        // La recherche est la seule à devenir une chaîne vide : la requête l'injecte dans un
        // like '%...%', où le vide laisse tout passer.
        assertEquals("", query.search());
        assertNull(query.genre());
        assertNull(query.platform());
        assertFalse(query.hasFilters());
    }

    @Test
    @DisplayName("Les filtres sont débarrassés de leurs espaces de bord")
    void trimsValues() {
        RankingQuery query = new RankingQuery(" zelda ", " Aventure ", " Switch ", null, null, null);

        assertEquals("zelda", query.search());
        assertEquals("Aventure", query.genre());
        assertEquals("Switch", query.platform());
        assertTrue(query.hasFilters());
    }

    @Test
    @DisplayName("Un intervalle d'années à l'envers est remis à l'endroit")
    void swapsInvertedYears() {
        RankingQuery query = new RankingQuery(null, null, null, 2024, 2010, null);

        // Laissé tel quel, cet intervalle ne renverrait jamais rien et l'utilisateur resterait
        // devant une page vide sans comprendre pourquoi.
        assertEquals(2010, query.yearFrom());
        assertEquals(2024, query.yearTo());
    }

    @Test
    @DisplayName("Les années aberrantes sont ramenées dans des bornes tenables")
    void clampsYears() {
        RankingQuery query = new RankingQuery(null, null, null, 1, 999_999, null);

        assertEquals(1950, query.yearFrom());
        assertEquals(2100, query.yearTo());
    }

    @Test
    @DisplayName("Les années deviennent les secondes Unix que la base stocke")
    void convertsYearsToEpochSeconds() {
        RankingQuery query = new RankingQuery(null, null, null, 2020, 2020, null);

        assertEquals(LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond(),
                query.releasedAfter());
        // La borne haute couvre le dernier instant de l'année : s'arrêter au 31 décembre à
        // minuit écarterait tous les jeux sortis ce jour-là.
        assertEquals(LocalDate.of(2020, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).getEpochSecond(),
                query.releasedBefore());
    }

    @Test
    @DisplayName("Sans année, aucune borne de date n'est posée")
    void noYearMeansNoBound() {
        RankingQuery query = new RankingQuery("zelda", null, null, null, null, null);

        assertNull(query.releasedAfter());
        assertNull(query.releasedBefore());
    }

    @Test
    @DisplayName("Un tri inconnu retombe sur le classement par défaut du site")
    void unknownSortFallsBackToWeighted() {
        assertEquals(RankingSort.WEIGHTED, RankingSort.parse("n'importe quoi"));
        assertEquals(RankingSort.WEIGHTED, RankingSort.parse(null));
        assertEquals(RankingSort.WEIGHTED, new RankingQuery(null, null, null, null, null, null).sort());
    }

    @Test
    @DisplayName("Un tri connu est reconnu quelle que soit sa casse")
    void parsesKnownSorts() {
        assertEquals(RankingSort.AVERAGE, RankingSort.parse("average"));
        assertEquals(RankingSort.VOTES, RankingSort.parse(" Votes "));
        assertEquals(RankingSort.TITLE, RankingSort.parse("TITLE"));
    }
}
