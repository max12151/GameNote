package be.technifutur.bll.rating;

import be.technifutur.dal.rating.GameStatus;
import be.technifutur.dal.rating.StatusCountView;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Combien de jeux ce joueur a rangés dans chaque statut.
 * <p>
 * Un enregistrement à trois champs plutôt qu'une carte : le front affiche trois onglets
 * dont les noms sont connus à l'avance, et un statut jamais employé doit s'y lire zéro
 * plutôt que d'être absent.
 */
public record LibrarySummary(long wishlist, long playing, long finished) {

    public static LibrarySummary from(List<StatusCountView> counts) {
        Map<GameStatus, Long> byStatus = counts.stream()
                .collect(Collectors.toMap(StatusCountView::getStatus,
                        StatusCountView::getCount,
                        Long::sum,
                        () -> new java.util.EnumMap<>(GameStatus.class)));

        Function<GameStatus, Long> count = status -> byStatus.getOrDefault(status, 0L);

        return new LibrarySummary(
                count.apply(GameStatus.WISHLIST),
                count.apply(GameStatus.PLAYING),
                count.apply(GameStatus.FINISHED)
        );
    }

    public long total() {
        return wishlist + playing + finished;
    }
}
