package be.technifutur.il.activity;

import be.technifutur.bll.activity.ActivityEntry;
import be.technifutur.dl.activity.ActivityEntryDto;
import be.technifutur.dl.activity.ActivityKindDto;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ActivityMapper {

    /**
     * @param reactedCommentIds avis que le lecteur a déjà marqués comme utiles, ramenés en
     *                          une requête pour toute la page du fil
     */
    public ActivityEntryDto toDto(ActivityEntry entry, Set<Long> reactedCommentIds) {
        return new ActivityEntryDto(
                ActivityKindDto.valueOf(entry.kind().name()),
                entry.at(),
                entry.authorId(),
                entry.authorUsername(),
                entry.authorHasAvatar(),
                entry.igdbGameId(),
                entry.gameTitle(),
                entry.gameCoverUrl(),
                entry.rating(),
                entry.content(),
                entry.commentId(),
                entry.usefulCount(),
                entry.commentId() != null && reactedCommentIds.contains(entry.commentId())
        );
    }
}
