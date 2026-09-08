package be.technifutur.bll.rating;

import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.rating.CommunityGameView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.GameRatingRepository;
import be.technifutur.dal.rating.GameStatus;
import be.technifutur.dal.rating.UserRatingCountView;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La bibliothèque d'un joueur : ce qu'il veut jouer, ce qu'il joue, ce qu'il a terminé ou
 * abandonné, et la note qu'il met à ce qu'il a joué.
 * <p>
 * Une entrée peut exister sans note — une envie n'est pas un avis. Les statistiques et le
 * classement ne considèrent donc que les entrées notées.
 */
@Service
public class GameRatingService {

    private static final int GENRE_BREAKDOWN_SIZE = 5;

    /** En deçà d'un demi-point d'écart, on considère que l'avis rejoint celui du site. */
    private static final double ALIGNMENT_THRESHOLD = 0.5;

    private final GameRatingRepository gameRatingRepository;

    public GameRatingService(GameRatingRepository gameRatingRepository) {
        this.gameRatingRepository = gameRatingRepository;
    }

    /**
     * Note un jeu, en créant l'entrée de bibliothèque si elle n'existe pas encore.
     *
     * @param status statut voulu, ou {@code null} pour conserver celui de l'entrée
     *               existante — et {@link GameStatus#FINISHED} pour une entrée créée à
     *               l'occasion, puisqu'on note un jeu après y avoir joué
     */
    @Transactional
    public GameRatingEntity rateGame(Long userId, GameMetadata game, int rating, GameStatus status) {
        GameRatingEntity entity = findOrCreate(userId, game.igdbGameId(), GameStatus.FINISHED);

        applyMetadata(entity, game);
        entity.setRating(rating);

        if (status != null) {
            entity.setStatus(status);
        }

        return gameRatingRepository.save(entity);
    }

    /**
     * Range un jeu dans la bibliothèque sous un statut donné, sans le noter.
     * <p>
     * Crée l'entrée si le jeu n'y était pas — c'est le geste « ajouter à ma liste d'envies »
     * depuis la recherche — et se contente de déplacer l'entrée existante sinon. La note
     * éventuelle est conservée : avoir remis un jeu déjà noté dans ses envies ne doit pas
     * effacer l'avis qu'on en avait.
     */
    @Transactional
    public GameRatingEntity setStatus(Long userId, GameMetadata game, GameStatus status) {
        GameRatingEntity entity = findOrCreate(userId, game.igdbGameId(), status);

        applyMetadata(entity, game);
        entity.setStatus(status);

        return gameRatingRepository.save(entity);
    }

    private GameRatingEntity findOrCreate(Long userId, Long igdbGameId, GameStatus statusIfNew) {
        return gameRatingRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseGet(() -> {
                    GameRatingEntity created = new GameRatingEntity();
                    created.setUserId(userId);
                    created.setIgdbGameId(igdbGameId);
                    created.setStatus(statusIfNew);
                    created.setCreatedAt(OffsetDateTime.now());
                    return created;
                });
    }

    /**
     * Rafraîchit les métadonnées à chaque enregistrement : elles viennent d'IGDB, qui corrige
     * ses fiches, et une entrée créée par un client d'une version antérieure peut en manquer.
     */
    private static void applyMetadata(GameRatingEntity entity, GameMetadata game) {
        entity.setTitle(game.title());
        entity.setCoverUrl(game.coverUrl());
        entity.setReleaseDate(game.releaseDate());
        entity.setSummary(game.summary());
        entity.setGenres(game.genres());
        entity.setDevelopers(game.developers());
        entity.setPublishers(game.publishers());
        entity.setPlatforms(game.platforms());
    }

    /** Toute la bibliothèque, notes et envies mêlées, de la plus récente à la plus ancienne. */
    public List<GameRatingEntity> getRatingsForUser(Long userId) {
        return gameRatingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /** La bibliothèque restreinte à un statut : la liste d'envies, les jeux en cours. */
    public List<GameRatingEntity> getLibrary(Long userId, GameStatus status) {
        return status == null
                ? getRatingsForUser(userId)
                : gameRatingRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, status);
    }

    public LibrarySummary getLibrarySummary(Long userId) {
        return LibrarySummary.from(gameRatingRepository.countByStatusForUser(userId));
    }

    /**
     * Jeux à ne plus proposer à ce joueur dans la page Découvrir : tout ce qui est déjà dans
     * sa bibliothèque, noté ou seulement mis de côté. Reproposer un jeu qu'on a soi-même
     * ajouté à ses envies n'aurait aucun sens.
     */
    public List<Long> getRatedIgdbGameIds(Long userId) {
        return gameRatingRepository.findIgdbGameIdsByUserId(userId);
    }

    /**
     * Nombre de jeux notés par chacun des membres donnés, en une requête pour toute la
     * liste plutôt qu'une par ligne affichée. Un membre qui n'a rien noté est absent de la
     * carte renvoyée : c'est à l'appelant de lire zéro.
     */
    public Map<Long, Long> countRatingsByUser(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        return gameRatingRepository.countRatingsByUserIds(userIds).stream()
                .collect(Collectors.toMap(UserRatingCountView::getUserId,
                        UserRatingCountView::getRatedGames));
    }

    public Optional<GameRatingEntity> findRating(Long userId, Long igdbGameId) {
        return gameRatingRepository.findByUserIdAndIgdbGameId(userId, igdbGameId);
    }

    /** Retire complètement le jeu de la bibliothèque : la note et le statut partent ensemble. */
    @Transactional
    public void removeRating(Long userId, Long igdbGameId) {
        GameRatingEntity entity = gameRatingRepository.findByUserIdAndIgdbGameId(userId, igdbGameId)
                .orElseThrow(() -> new ResourceNotFoundException("Ce jeu n'est pas dans votre bibliothèque"));

        gameRatingRepository.delete(entity);
    }

    /**
     * Statistiques de notation d'un joueur.
     * <p>
     * Seules les entrées notées comptent : une liste d'envies de cent titres ne dit rien de la
     * sévérité de quelqu'un, et la faire entrer dans la moyenne n'aurait pas de sens.
     */
    public RatingStats getStats(Long userId) {
        List<GameRatingEntity> ratings = getRatingsForUser(userId).stream()
                .filter(GameRatingEntity::isRated)
                .toList();

        if (ratings.isEmpty()) {
            return new RatingStats(0, null, null, 0, null, List.of(), List.of(), TasteComparison.empty());
        }

        double average = ratings.stream()
                .mapToInt(GameRatingEntity::getRating)
                .average()
                .orElse(0);

        List<GenreCount> genreBreakdown = ratings.stream()
                .flatMap(r -> r.getGenres() == null ? List.<String>of().stream() : r.getGenres().stream())
                .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> new GenreCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingLong(GenreCount::count).reversed())
                .limit(GENRE_BREAKDOWN_SIZE)
                .toList();

        GenreCount topGenre = genreBreakdown.isEmpty() ? null : genreBreakdown.get(0);

        GameRatingEntity bestRatedGame = ratings.stream()
                .max(Comparator.<GameRatingEntity>comparingInt(GameRatingEntity::getRating)
                        .thenComparing(GameRatingEntity::getCreatedAt))
                .orElse(null);

        return new RatingStats(
                ratings.size(),
                average,
                topGenre != null ? topGenre.genre() : null,
                topGenre != null ? (int) topGenre.count() : 0,
                bestRatedGame,
                genreBreakdown,
                buildDistribution(ratings),
                compareWithCommunity(ratings)
        );
    }

    /**
     * Histogramme des notes de l'utilisateur, calculé sur la liste déjà chargée : aucune
     * requête supplémentaire n'est nécessaire.
     */
    private List<RatingBucket> buildDistribution(List<GameRatingEntity> ratings) {
        Map<Integer, Long> counts = ratings.stream()
                .collect(Collectors.groupingBy(GameRatingEntity::getRating, Collectors.counting()));

        return IntStream.rangeClosed(1, 10)
                .mapToObj(note -> new RatingBucket(note, counts.getOrDefault(note, 0L)))
                .toList();
    }

    /**
     * Situe l'utilisateur par rapport au reste du site : note-t-il plus sévèrement, plus
     * généreusement, ou comme les autres ? Une seule requête agrégée ramène les moyennes
     * de tous ses jeux d'un coup.
     */
    private TasteComparison compareWithCommunity(List<GameRatingEntity> ratings) {
        List<Long> igdbGameIds = ratings.stream().map(GameRatingEntity::getIgdbGameId).toList();

        Map<Long, Double> siteAverages = gameRatingRepository.findCommunityAveragesFor(igdbGameIds).stream()
                .filter(view -> view.getAverageRating() != null)
                .collect(Collectors.toMap(CommunityGameView::getIgdbGameId, CommunityGameView::getAverageRating));

        if (siteAverages.isEmpty()) {
            return TasteComparison.empty();
        }

        double totalDelta = 0;
        int compared = 0;
        int stricter = 0;
        int aligned = 0;
        int generous = 0;

        for (GameRatingEntity rating : ratings) {
            Double siteAverage = siteAverages.get(rating.getIgdbGameId());
            if (siteAverage == null) {
                continue;
            }

            double delta = rating.getRating() - siteAverage;
            totalDelta += delta;
            compared++;

            if (delta <= -ALIGNMENT_THRESHOLD) {
                stricter++;
            } else if (delta >= ALIGNMENT_THRESHOLD) {
                generous++;
            } else {
                aligned++;
            }
        }

        if (compared == 0) {
            return TasteComparison.empty();
        }

        return new TasteComparison(totalDelta / compared, compared, stricter, aligned, generous);
    }
}
