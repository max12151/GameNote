package be.technifutur.bll.activity;

import be.technifutur.dal.comment.GameCommentRepository;
import be.technifutur.dal.rating.GameRatingRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Le fil d'activité des joueurs suivis : leurs notes et leurs avis, du plus récent au plus
 * ancien.
 * <p>
 * Les deux sources vivent dans deux tables et se lisent par deux requêtes, puis sont fondues
 * et triées en mémoire. Une requête unique — un {@code union} — aurait demandé du SQL natif
 * pour deux projections de formes différentes, sans rien gagner : chaque source est déjà
 * limitée à la fenêtre demandée.
 */
@Service
public class ActivityService {

    /** Plafond d'une page de fil : au-delà, personne ne fait défiler. */
    private static final int MAX_LIMIT = 31;

    /**
     * Profondeur maximale du fil. Chaque source doit ramener tout ce qui précède la position
     * demandée pour que la fusion soit correcte ; laisser filer cette fenêtre ferait grossir
     * la lecture à chaque page tournée.
     */
    private static final int MAX_OFFSET = 600;

    private final GameRatingRepository gameRatingRepository;
    private final GameCommentRepository gameCommentRepository;

    public ActivityService(GameRatingRepository gameRatingRepository,
                           GameCommentRepository gameCommentRepository) {
        this.gameRatingRepository = gameRatingRepository;
        this.gameCommentRepository = gameCommentRepository;
    }

    /**
     * Les {@code limit} entrées qui suivent la position {@code offset}, tous joueurs suivis
     * confondus.
     * <p>
     * Chaque source ramène la fenêtre entière — les {@code offset + limit} premières lignes —
     * avant la fusion : la vingtième entrée du fil peut très bien être le vingtième avis
     * alors qu'aucune note n'a été publiée entre-temps, et ne demander que {@code limit}
     * lignes à chacune produirait un fil qui saute des entrées.
     * <p>
     * L'appelant raisonne en position et non en page pour pouvoir demander une entrée de plus
     * que ce qu'il affiche : sa présence lui dit qu'il reste une page à charger, sans avoir à
     * compter deux tables.
     * <p>
     * Quand un joueur a noté un jeu <em>et</em> laissé son avis dessus, seul l'avis est
     * retenu : il porte déjà la note, et afficher les deux ferait lire deux fois la même
     * nouvelle.
     */
    public List<ActivityEntry> getFeed(Collection<Long> followedIds, int offset, int limit) {
        if (followedIds.isEmpty()) {
            return List.of();
        }

        int safeLimit = Math.clamp(limit, 1, MAX_LIMIT);
        int safeOffset = Math.clamp(offset, 0, MAX_OFFSET);
        int window = safeOffset + safeLimit;

        PageRequest windowRequest = PageRequest.of(0, window);

        List<ActivityEntry> entries = new ArrayList<>(window * 2);

        gameCommentRepository.findFeedViews(followedIds, windowRequest).stream()
                .map(ActivityEntry::ofComment)
                .forEach(entries::add);

        // Les jeux déjà représentés par un avis : leur note ne mérite pas une seconde ligne.
        Set<String> commented = new HashSet<>();
        for (ActivityEntry entry : entries) {
            commented.add(key(entry));
        }

        gameRatingRepository.findActivityForUsers(followedIds, windowRequest).stream()
                .map(ActivityEntry::ofRating)
                .filter(entry -> !commented.contains(key(entry)))
                .forEach(entries::add);

        entries.sort(Comparator.comparing(ActivityEntry::at).reversed());

        int from = Math.min(safeOffset, entries.size());
        int to = Math.min(from + safeLimit, entries.size());

        return List.copyOf(entries.subList(from, to));
    }

    /** Un jeu, vu par un joueur : ce qui fait qu'une note et un avis parlent de la même chose. */
    private static String key(ActivityEntry entry) {
        return entry.authorId() + ":" + entry.igdbGameId();
    }
}
