package be.technifutur.gamenote.api.igdb;

import java.util.List;

public record IgdbGameResult(
        long igdbId,
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
        Double aggregatedRating) {}
