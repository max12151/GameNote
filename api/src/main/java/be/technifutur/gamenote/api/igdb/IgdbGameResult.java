package be.technifutur.gamenote.api.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGameResult(
        Long id,
        String name,
        String summary,

        @JsonProperty("game_type")
        Long gameType,

        @JsonProperty("first_release_date")
        Long firstReleaseDate,

        Cover cover,
        List<Genre> genres,
        List<Platform> platforms,
        Double rating,

        @JsonProperty("aggregated_rating")
        Double aggregatedRating,

        @JsonProperty("involved_companies")
        List<InvolvedCompany> involvedCompanies,

        List<Artwork> artworks,
        List<Screenshot> screenshots
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(Long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Platform(Long id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Artwork(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Screenshot(String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InvolvedCompany(
            Company company,
            boolean developer,
            boolean publisher
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Company(Long id, String name) {
    }
}