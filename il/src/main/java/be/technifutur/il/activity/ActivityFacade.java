package be.technifutur.il.activity;

import be.technifutur.bll.activity.ActivityEntry;
import be.technifutur.bll.activity.ActivityService;
import be.technifutur.bll.follow.FollowService;
import be.technifutur.bll.reaction.CommentReactionService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.activity.ActivityEntryDto;
import be.technifutur.dl.activity.ActivityFeedDto;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Le fil d'activité des joueurs suivis.
 */
@Component
public class ActivityFacade {

    private static final int DEFAULT_SIZE = 15;

    /** Doit rester sous le plafond du service, qui ramène une entrée de plus pour le « voir plus ». */
    private static final int MAX_SIZE = 30;

    private final UserService userService;
    private final FollowService followService;
    private final ActivityService activityService;
    private final CommentReactionService commentReactionService;
    private final ActivityMapper activityMapper;

    public ActivityFacade(UserService userService,
                          FollowService followService,
                          ActivityService activityService,
                          CommentReactionService commentReactionService,
                          ActivityMapper activityMapper) {
        this.userService = userService;
        this.followService = followService;
        this.activityService = activityService;
        this.commentReactionService = commentReactionService;
        this.activityMapper = activityMapper;
    }

    /**
     * Une page du fil.
     * <p>
     * Le nombre de joueurs suivis accompagne la réponse : un fil vide ne dit pas la même
     * chose selon qu'on ne suit personne — il faut alors inviter à chercher des joueurs — ou
     * que ceux qu'on suit n'ont rien publié.
     */
    public ActivityFeedDto getFeed(String username, int page, int size) {
        UserEntity me = userService.getByUsername(username);

        List<Long> followedIds = followService.getFollowedIds(me.getId());

        // Le plafond est repris ici et pas seulement dans le service : demander mille entrées
        // en rendrait trente, et le « il en reste » se lirait faux — jamais rien de plus que
        // ce qui a été demandé, donc jamais de page suivante.
        int safeSize = Math.clamp(size <= 0 ? DEFAULT_SIZE : size, 1, MAX_SIZE);
        int safePage = Math.max(page, 0);

        // Une entrée de plus que demandé : sa présence dit qu'il reste une page, sans avoir à
        // compter deux tables pour un total que personne ne lit dans un fil.
        List<ActivityEntry> entries =
                activityService.getFeed(followedIds, safePage * safeSize, safeSize + 1);

        boolean hasMore = entries.size() > safeSize;
        List<ActivityEntry> visible = hasMore ? entries.subList(0, safeSize) : entries;

        Set<Long> commentIds = visible.stream()
                .map(ActivityEntry::commentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Set<Long> reacted = commentReactionService.getReactedCommentIds(me.getId(), commentIds);

        List<ActivityEntryDto> dtos = visible.stream()
                .map(entry -> activityMapper.toDto(entry, reacted))
                .toList();

        return new ActivityFeedDto(dtos, safePage, safeSize, hasMore, followedIds.size());
    }
}
