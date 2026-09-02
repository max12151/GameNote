package be.technifutur.il.community;

import be.technifutur.bll.community.CommunityGameDetail;
import be.technifutur.bll.community.CommunityRanking;
import be.technifutur.dal.rating.CommunityRankingView;
import be.technifutur.dal.rating.GameRatingEntity;
import be.technifutur.dal.rating.RatingBucketView;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityGameDto;
import be.technifutur.dl.community.CommunityGameInfoDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.dl.community.RatingBucketDto;
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
                ranking.weights().minimumVotes()
        );
    }

    public CommunityGameDetailDto toDetailDto(CommunityGameDetail detail,
                                              List<GameCommentDto> comments,
                                              Integer myRating,
                                              GameCommentDto myComment) {
        return new CommunityGameDetailDto(
                toInfoDto(detail.game()),
                detail.averageRating(),
                detail.ratingCount(),
                detail.distribution().stream().map(this::toBucketDto).toList(),
                comments,
                myRating,
                myComment
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
