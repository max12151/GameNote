package be.technifutur.bll.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import be.technifutur.dal.activity.ActivityKind;
import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.comment.RecentGameCommentView;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.RatingActivityView;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * La fusion des deux sources du fil d'activité.
 * <p>
 * Notes et avis viennent de deux tables et de deux requêtes. Les recoller dans le bon ordre
 * est tout le travail de ce service : une erreur ici ne casse rien, elle produit un fil où
 * les nouvelles arrivent dans le désordre, ou en double.
 */
class ActivityServiceTest {

    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-07T12:00:00Z");
    private static final List<Long> FOLLOWED = List.of(1L, 2L);

    private GameRatingRepository ratingRepository;
    private GameCommentRepository commentRepository;
    private ActivityService service;

    @BeforeEach
    void setUp() {
        ratingRepository = mock(GameRatingRepository.class);
        commentRepository = mock(GameCommentRepository.class);
        service = new ActivityService(ratingRepository, commentRepository);
    }

    @Test
    @DisplayName("Sans personne à suivre, le fil est vide et aucune requête n'est faite")
    void emptyWithoutFollowedPlayers() {
        assertTrue(service.getFeed(List.of(), 0, 10).isEmpty());
    }

    @Test
    @DisplayName("Notes et avis sont entrelacés par date, du plus récent au plus ancien")
    void mergesBothSourcesByDate() {
        givenComments(comment(10L, 1L, 500L, NOW.minusHours(3)));
        givenRatings(rating(1L, 501L, NOW.minusHours(1)), rating(2L, 502L, NOW.minusHours(5)));

        List<ActivityEntry> feed = service.getFeed(FOLLOWED, 0, 10);

        assertEquals(List.of(501L, 500L, 502L), feed.stream().map(ActivityEntry::igdbGameId).toList());
        assertEquals(ActivityKind.RATING, feed.get(0).kind());
        assertEquals(ActivityKind.COMMENT, feed.get(1).kind());
    }

    @Test
    @DisplayName("Un jeu noté puis commenté par le même joueur ne compte qu'une fois")
    void keepsCommentOverRatingForTheSameGame() {
        givenComments(comment(10L, 1L, 500L, NOW.minusHours(1)));
        givenRatings(rating(1L, 500L, NOW.minusHours(2)));

        List<ActivityEntry> feed = service.getFeed(FOLLOWED, 0, 10);

        // L'avis porte déjà la note de son auteur : afficher les deux ferait lire deux fois
        // la même nouvelle.
        assertEquals(1, feed.size());
        assertEquals(ActivityKind.COMMENT, feed.get(0).kind());
    }

    @Test
    @DisplayName("Le même jeu noté par deux joueurs différents donne bien deux entrées")
    void doesNotCollapseAcrossAuthors() {
        givenComments();
        givenRatings(rating(1L, 500L, NOW.minusHours(1)), rating(2L, 500L, NOW.minusHours(2)));

        assertEquals(2, service.getFeed(FOLLOWED, 0, 10).size());
    }

    @Test
    @DisplayName("La position demandée découpe le fil déjà fusionné")
    void appliesOffsetAfterMerging() {
        givenComments(comment(10L, 1L, 500L, NOW.minusHours(2)));
        givenRatings(rating(1L, 501L, NOW.minusHours(1)), rating(2L, 502L, NOW.minusHours(3)));

        // Découper chaque source avant la fusion ferait sauter des entrées : la deuxième du
        // fil est un avis, alors qu'une note la précède et une autre la suit.
        assertEquals(List.of(500L),
                service.getFeed(FOLLOWED, 1, 1).stream().map(ActivityEntry::igdbGameId).toList());
        assertEquals(List.of(502L),
                service.getFeed(FOLLOWED, 2, 1).stream().map(ActivityEntry::igdbGameId).toList());
    }

    private void givenComments(RecentGameCommentView... views) {
        when(commentRepository.findFeedViews(anyCollection(), any())).thenReturn(List.of(views));
    }

    private void givenRatings(RatingActivityView... views) {
        when(ratingRepository.findActivityForUsers(anyCollection(), any())).thenReturn(List.of(views));
    }

    private static RatingActivityView rating(long authorId, long igdbGameId, OffsetDateTime at) {
        RatingActivityView view = mock(RatingActivityView.class);
        when(view.getId()).thenReturn(igdbGameId);
        when(view.getIgdbGameId()).thenReturn(igdbGameId);
        when(view.getGameTitle()).thenReturn("Jeu " + igdbGameId);
        when(view.getGameCoverUrl()).thenReturn(null);
        when(view.getRating()).thenReturn(8);
        when(view.getActivityAt()).thenReturn(at);
        when(view.getAuthorId()).thenReturn(authorId);
        when(view.getAuthorUsername()).thenReturn("joueur" + authorId);
        when(view.getAuthorHasAvatar()).thenReturn(false);
        return view;
    }

    private static RecentGameCommentView comment(long id, long authorId, long igdbGameId, OffsetDateTime at) {
        RecentGameCommentView view = mock(RecentGameCommentView.class);
        when(view.getId()).thenReturn(id);
        when(view.getIgdbGameId()).thenReturn(igdbGameId);
        when(view.getContent()).thenReturn("Un avis");
        when(view.getCreatedAt()).thenReturn(at);
        when(view.getAuthorId()).thenReturn(authorId);
        when(view.getAuthorUsername()).thenReturn("joueur" + authorId);
        when(view.getAuthorHasAvatar()).thenReturn(false);
        when(view.getAuthorRating()).thenReturn(9);
        when(view.getGameTitle()).thenReturn("Jeu " + igdbGameId);
        when(view.getGameCoverUrl()).thenReturn(null);
        when(view.getUsefulCount()).thenReturn(0L);
        return view;
    }
}
