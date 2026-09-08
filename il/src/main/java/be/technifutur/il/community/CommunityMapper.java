package be.technifutur.il.community;

import be.technifutur.bll.community.CommunityGameDetail;
import be.technifutur.bll.community.CommunityRanking;
import be.technifutur.bll.community.RankingFacets;
import be.technifutur.bll.community.RankingQuery;
import be.technifutur.dal.rating.CommunityRankingView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.RatingBucketView;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityGameDto;
import be.technifutur.dl.community.CommunityGameInfoDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.dl.community.RankingFacetsDto;
import be.technifutur.dl.community.RankingFiltersDto;
import be.technifutur.dl.community.RatingBucketDto;
import be.technifutur.dl.rating.GameStatusDto;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CommunityMapper {

    public CommunityRankingDto toRankingDto(CommunityRanking ranking, Map<Long, Long> commentCounts) {
        List<CommunityGameDto> games = ranking.games().stream()
                .map(game -> toGameDto(game, commentCounts.getOrDefault(game.getIgdbGameId(), 0L)))
                .toList();

        return new CommunityRankingDto(
                games,
                ranking.totalGames(),
                ranking.page(),
                ranking.size(),
                ranking.weights().globalAverage(),
                ranking.weights().minimumVotes(),
                toFiltersDto(ranking.query())
        );
    }

    /**
     * Les filtres tels que le serveur les a compris. Renvoyer la demande normalisée plutôt
     * que rien permet au front de refléter l'état réel du classement — une année aberrante ou
     * un intervalle à l'envers ont pu être corrigés en chemin.
     */
    public RankingFiltersDto toFiltersDto(RankingQuery query) {
        return new RankingFiltersDto(
                query.search(),
                query.genre(),
                query.platform(),
                query.yearFrom(),
                query.yearTo(),
                query.sort().name()
        );
    }

    public RankingFacetsDto toFacetsDto(RankingFacets facets) {
        return new RankingFacetsDto(facets.genres(), facets.platforms());
    }

    public CommunityGameDetailDto toDetailDto(CommunityGameDetail detail,
                                              List<GameCommentDto> comments,
                                              Integer myRating,
                                              GameCommentDto myComment,
                                              GameStatusDto myStatus,
                                              List<Long> myListIds,
                                              String commentSort) {
        return new CommunityGameDetailDto(
                toInfoDto(detail.game()),
                detail.averageRating(),
                detail.ratingCount(),
                detail.distribution().stream().map(this::toBucketDto).toList(),
                comments,
                myRating,
                myComment,
                myStatus,
                myListIds,
                commentSort
        );
    }

    private CommunityGameDto toGameDto(CommunityRankingView view, long commentCount) {
        return new CommunityGameDto(
                view.getIgdbGameId(),
                view.getTitle(),
                view.getCoverUrl(),
                view.getReleaseDate(),
                view.getAverageRating() != null ? view.getAverageRating() : 0,
                view.getWeightedRating() != null ? view.getWeightedRating() : 0,
                view.getRatingCount(),
                commentCount
        );
    }

    private CommunityGameInfoDto toInfoDto(GameRatingEntity entity) {
        return new CommunityGameInfoDto(
                entity.getIgdbGameId(),
                entity.getTitle(),
                entity.getCoverUrl(),
                entity.getReleaseDate(),
                entity.getSummary(),
                nullSafe(entity.getGenres()),
                nullSafe(entity.getDevelopers()),
                nullSafe(entity.getPublishers()),
                nullSafe(entity.getPlatforms())
        );
    }

    private RatingBucketDto toBucketDto(RatingBucketView bucket) {
        return new RatingBucketDto(bucket.getRating(), bucket.getCount());
    }

    private List<String> nullSafe(List<String> values) {
        return values != null ? List.copyOf(values) : List.of();
    }
}
