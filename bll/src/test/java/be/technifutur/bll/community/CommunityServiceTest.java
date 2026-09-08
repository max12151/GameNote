package be.technifutur.bll.community;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.GameVoteView;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Les deux constantes de la pondération bayésienne du classement.
 * <p>
 * Elles ne sont pas figées : elles se recalculent sur l'état réel du site à chaque appel.
 * C'est ce qui rend le classement correct aussi bien sur un site où chaque jeu a trois
 * votes que sur un site où il en a trois cents — et c'est exactement pour cela qu'elles
 * méritent un test : une erreur ici ne casse rien, elle réordonne silencieusement la page
 * la plus visible du site.
 */
class CommunityServiceTest {

    private GameRatingRepository repository;
    private CommunityService service;

    @BeforeEach
    void setUp() {
        repository = mock(GameRatingRepository.class);
        service = new CommunityService(repository);

        when(repository.findCommunityRanking(anyString(), nullable(String.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class), anyString(), anyDouble(), anyDouble(), any()))
                .thenReturn(List.of());
        when(repository.countRatedGames(anyString(), nullable(String.class), nullable(String.class),
                nullable(Long.class), nullable(Long.class))).thenReturn(0L);
    }

    /** Une demande sans filtre ni tri : ce que sert la page du classement par défaut. */
    private static RankingQuery anyQuery() {
        return new RankingQuery(null, null, null, null, null, null);
    }

    @Test
    @DisplayName("Un site sans aucune note ne fait pas planter le calcul")
    void handlesEmptySite() {
        when(repository.findVotesPerGame()).thenReturn(List.of());

        RankingWeights weights = service.getRanking(0, 20, anyQuery()).weights();

        assertEquals(0, weights.globalAverage());
        // Un seuil nul annulerait la pondération : la formule rendrait la moyenne brute.
        assertEquals(1, weights.minimumVotes());
    }

    @Test
    @DisplayName("Le seuil est la médiane des votes, pas leur moyenne")
    void usesMedianRatherThanMean() {
        // Un jeu très commenté (100 votes) tirerait la moyenne à 21 et pénaliserait tous
        // les autres ; la médiane, elle, reste à 3.
        when(repository.findVotesPerGame()).thenReturn(List.of(
                vote(1, 8), vote(2, 6), vote(3, 7), vote(4, 9), vote(100, 5)));

        assertEquals(3, service.getRanking(0, 20, anyQuery()).weights().minimumVotes());
    }

    @Test
    @DisplayName("Sur un nombre pair de jeux, la médiane est la moyenne des deux valeurs centrales")
    void averagesTheTwoMiddleValues() {
        when(repository.findVotesPerGame()).thenReturn(List.of(
                vote(2, 6), vote(4, 7), vote(6, 8), vote(10, 9)));

        assertEquals(5, service.getRanking(0, 20, anyQuery()).weights().minimumVotes());
    }

    @Test
    @DisplayName("La moyenne du site est la moyenne des moyennes, chaque jeu comptant pour un")
    void averagesPerGameNotPerVote() {
        when(repository.findVotesPerGame()).thenReturn(List.of(vote(1, 4), vote(50, 8)));

        assertEquals(6, service.getRanking(0, 20, anyQuery()).weights().globalAverage());
    }

    @Test
    @DisplayName("Page et taille demandées sont ramenées dans des bornes tenables")
    void clampsPagination() {
        when(repository.findVotesPerGame()).thenReturn(List.of());

        assertEquals(0, service.getRanking(-3, 20, anyQuery()).page());
        assertEquals(1, service.getRanking(0, 0, anyQuery()).size());
        assertEquals(50, service.getRanking(0, 5_000, anyQuery()).size());
    }

    /**
     * Une ligne « (votes, moyenne) » d'un jeu. Implémentée directement plutôt que simulée :
     * la projection n'a que deux accesseurs, et un mock construit à l'intérieur d'un
     * {@code when(...)} en cours de définition fait échouer Mockito.
     */
    private record Vote(long ratingCount, Double averageRating) implements GameVoteView {

        @Override
        public long getRatingCount() {
            return ratingCount;
        }

        @Override
        public Double getAverageRating() {
            return averageRating;
        }
    }

    private static GameVoteView vote(long count, double average) {
        return new Vote(count, average);
    }
}
