package be.technifutur.il.discover;

import be.technifutur.bll.discover.GameSkipService;
import be.technifutur.bll.rating.GameRatingService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class DiscoverFacade {

    private final UserService userService;
    private final GameRatingService gameRatingService;
    private final GameSkipService gameSkipService;

    public DiscoverFacade(UserService userService,
                          GameRatingService gameRatingService,
                          GameSkipService gameSkipService) {
        this.userService = userService;
        this.gameRatingService = gameRatingService;
        this.gameSkipService = gameSkipService;
    }

    /**
     * Jeux à ne pas reproposer à cet utilisateur : ceux qu'il a déjà notés, et ceux qu'il a
     * passés il y a moins que le délai de réapparition.
     */
    public Set<Long> getExcludedGameIds(String username) {
        UserEntity user = userService.getByUsername(username);

        List<Long> rated = gameRatingService.getRatedIgdbGameIds(user.getId());
        List<Long> skipped = gameSkipService.getActiveSkippedGameIds(user.getId());

        Set<Long> excluded = HashSet.newHashSet(rated.size() + skipped.size());
        excluded.addAll(rated);
        excluded.addAll(skipped);

        return excluded;
    }

    public void skipGame(String username, Long igdbGameId) {
        UserEntity user = userService.getByUsername(username);
        gameSkipService.skip(user.getId(), igdbGameId);
    }
}
