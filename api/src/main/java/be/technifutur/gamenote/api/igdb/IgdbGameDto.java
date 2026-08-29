package be.technifutur.gamenote.api.igdb;

import java.util.List;
import java.util.Objects;

public record IgdbGameDto(
        Long igdbId,
        String title,
        String summary,
        Long firstReleaseDate,
        String coverUrl,
        List<String> artworkUrls,
        List<String> screenshotUrls,
        List<String> genres,
        List<String> platforms,
        List<String> developers,
        List<String> publishers,
        Double rating,
        Double aggregatedRating,
        Long hypes
) {
    public static IgdbGameDto from(IgdbGameResult r) {
        return new IgdbGameDto(
                r.id(),
                r.name(),
                r.summary(),
                r.firstReleaseDate(),
                normalizeCoverUrl(r.cover() != null ? r.cover().url() : null),
                mapUrls(r.artworks(), IgdbGameResult.Artwork::url),
                mapUrls(r.screenshots(), IgdbGameResult.Screenshot::url),
                mapNames(r.genres(), IgdbGameResult.Genre::name),
                mapNames(r.platforms(), IgdbGameResult.Platform::name),
                companiesOfType(r.involvedCompanies(), true),
                companiesOfType(r.involvedCompanies(), false),
                r.rating(),
                r.aggregatedRating(),
                r.hypes()
        );
    }

    private static List<String> companiesOfType(List<IgdbGameResult.InvolvedCompany> companies, boolean developer) {
        if (companies == null) return List.of();
        return companies.stream()
                .filter(c -> developer ? c.developer() : c.publisher())
                .map(c -> c.company() != null ? c.company().name() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    private static <T> List<String> mapNames(List<T> items, java.util.function.Function<T, String> nameFn) {
        if (items == null) return List.of();
        return items.stream().map(nameFn).filter(Objects::nonNull).toList();
    }

    private static <T> List<String> mapUrls(List<T> items, java.util.function.Function<T, String> urlFn) {
        if (items == null) return List.of();
        return items.stream()
                .map(urlFn)
                .map(IgdbGameDto::normalizeUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String normalizeUrl(String url) {
        if (url == null) return null;
        return url.startsWith("//") ? "https:" + url : url;
    }

    private static String normalizeCoverUrl(String url) {
        String normalized = normalizeUrl(url);
        return normalized != null ? normalized.replace("t_thumb", "t_cover_big") : null;
    }
}