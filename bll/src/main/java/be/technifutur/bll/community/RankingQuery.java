package be.technifutur.bll.community;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Ce qu'on demande au classement : une recherche, des filtres, un ordre.
 * <p>
 * Un champ nul neutralise son filtre. Les années sont converties en secondes Unix parce que
 * c'est ainsi qu'IGDB donne les dates de sortie et que la base les stocke : la conversion se
 * fait ici, une fois, plutôt que dans la requête.
 *
 * @param yearFrom première année incluse, ou null
 * @param yearTo   dernière année incluse, ou null
 */
public record RankingQuery(String search,
                           String genre,
                           String platform,
                           Integer yearFrom,
                           Integer yearTo,
                           RankingSort sort) {

    /** Bornes acceptées : le jeu vidéo n'existait pas avant, et la suite reste à écrire. */
    private static final int MIN_YEAR = 1950;
    private static final int MAX_YEAR = 2100;

    public RankingQuery {
        String cleanedSearch = normalize(search);

        search = cleanedSearch == null ? "" : cleanedSearch;
        genre = normalize(genre);
        platform = normalize(platform);
        yearFrom = clampYear(yearFrom);
        yearTo = clampYear(yearTo);
        sort = sort == null ? RankingSort.WEIGHTED : sort;

        // Un intervalle à l'envers ne renverrait jamais rien : on le remet à l'endroit plutôt
        // que de laisser l'utilisateur devant une page vide sans explication.
        if (yearFrom != null && yearTo != null && yearFrom > yearTo) {
            Integer swap = yearFrom;
            yearFrom = yearTo;
            yearTo = swap;
        }
    }

    /** Premier instant de l'année de départ, en secondes Unix. */
    public Long releasedAfter() {
        return yearFrom == null
                ? null
                : LocalDate.of(yearFrom, 1, 1).atStartOfDay(ZoneOffset.UTC).toEpochSecond();
    }

    /** Dernier instant de l'année de fin, en secondes Unix. */
    public Long releasedBefore() {
        return yearTo == null
                ? null
                : LocalDate.of(yearTo, 12, 31).atTime(23, 59, 59).toInstant(ZoneOffset.UTC).getEpochSecond();
    }

    public boolean hasFilters() {
        return genre != null || platform != null || yearFrom != null || yearTo != null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }

        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    private static Integer clampYear(Integer year) {
        return year == null ? null : Math.clamp(year, MIN_YEAR, MAX_YEAR);
    }
}
