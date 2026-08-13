package be.technifutur.gamenote.api.igdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IgdbGameResult(
        Long id,
        String name,
        String summary,

        @JsonProperty("first_release_date")
        Long firstReleaseDate,

        Cover cover
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cover(
            String url
    ) {
    }
}