package be.technifutur.gamenote.api.activity;

import be.technifutur.dl.activity.ActivityFeedDto;
import be.technifutur.il.activity.ActivityFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Le fil d'activité des joueurs suivis : leurs notes et leurs avis, du plus récent au plus
 * ancien.
 */
@RestController
@RequestMapping("/api/feed")
public class ActivityController {

    private final ActivityFacade activityFacade;

    public ActivityController(ActivityFacade activityFacade) {
        this.activityFacade = activityFacade;
    }

    @GetMapping
    public ResponseEntity<ActivityFeedDto> getFeed(Authentication authentication,
                                                   @RequestParam(defaultValue = "0") int page,
                                                   @RequestParam(defaultValue = "15") int size) {
        return ResponseEntity.ok(activityFacade.getFeed(authentication.getName(), page, size));
    }
}
