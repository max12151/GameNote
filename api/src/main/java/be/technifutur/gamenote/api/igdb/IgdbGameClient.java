package be.technifutur.gamenote.api.igdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;

@Service
public class IgdbGameClient {
    private final RestClient igdb;
    private final IgdbProperties properties;
    private final IgdbTokenService tokens;

    public IgdbGameClient(RestClient igdbRestClient, IgdbProperties properties, IgdbTokenService tokens) {
        this.igdb = igdbRestClient;
        this.properties = properties;
        this.tokens = tokens;
    }

    public List<IgdbGameResult> search(String title, int requestedLimit) {
        var normalizedTitle = title == null ? "" : title.trim();
        if (normalizedTitle.isBlank()) return List.of();
        var limit = Math.min(Math.max(requestedLimit, 1), 20);
        var query = "fields id,name,summary,first_release_date,cover.image_id,artworks.image_id,"
                + "screenshots.image_id,genres.name,platforms.name,"
                + "involved_companies.company.name,involved_companies.developer,"
                + "involved_companies.publisher,rating,aggregated_rating; "
                + "search \\\"" + escape(normalizedTitle) + "\\\"; "
                + "where version_parent = null; limit " + limit + ";";

        var response = igdb.post().uri("/games")
                .header("Client-ID", properties.clientId())
                .header("Authorization", "Bearer " + tokens.token())
                .contentType(MediaType.TEXT_PLAIN)
                .accept(MediaType.APPLICATION_JSON)
                .body(query)
                .retrieve()
                .body(new ParameterizedTypeReference<List<IgdbGame>>() {});
        return response == null ? List.of() : response.stream().map(this::map).toList();
    }

    private IgdbGameResult map(IgdbGame game) {
        var developers = game.involvedCompanies() == null ? List.<String>of() : game.involvedCompanies().stream()
                .filter(InvolvedCompany::developer).map(InvolvedCompany::company).filter(Objects::nonNull)
                .map(Company::name).filter(Objects::nonNull).distinct().toList();
        var publishers = game.involvedCompanies() == null ? List.<String>of() : game.involvedCompanies().stream()
                .filter(InvolvedCompany::publisher).map(InvolvedCompany::company).filter(Objects::nonNull)
                .map(Company::name).filter(Objects::nonNull).distinct().toList();
        return new IgdbGameResult(game.id(), game.name(), game.summary(), game.firstReleaseDate(),
                cover(game.cover()), imageList(game.artworks(), "t_1080p"), imageList(game.screenshots(), "t_screenshot_big"),
                names(game.genres()), names(game.platforms()), developers, publishers, game.rating(), game.aggregatedRating());
    }

    private String cover(Cover cover) { return cover == null ? null : image(cover.imageId(), "t_cover_big"); }
    private List<String> imageList(List<Image> images, String size) {
        return images == null ? List.of() : images.stream().map(Image::imageId).filter(Objects::nonNull).map(id -> image(id, size)).toList();
    }
    private List<String> names(List<NamedValue> values) {
        return values == null ? List.of() : values.stream().map(NamedValue::name).filter(Objects::nonNull).toList();
    }
    private String image(String id, String size) { return "https://images.igdb.com/igdb/image/upload/" + size + "/" + id + ".jpg"; }
    private String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }

    private record IgdbGame(long id, String name, String summary,
                            @JsonProperty("first_release_date") Long firstReleaseDate,
                            Cover cover, List<Image> artworks, List<Image> screenshots,
                            List<NamedValue> genres, List<NamedValue> platforms,
                            @JsonProperty("involved_companies") List<InvolvedCompany> involvedCompanies,
                            Double rating, @JsonProperty("aggregated_rating") Double aggregatedRating) {}
    private record Cover(@JsonProperty("image_id") String imageId) {}
    private record Image(@JsonProperty("image_id") String imageId) {}
    private record NamedValue(String name) {}
    private record InvolvedCompany(Company company, boolean developer, boolean publisher) {}
    private record Company(String name) {}
}
