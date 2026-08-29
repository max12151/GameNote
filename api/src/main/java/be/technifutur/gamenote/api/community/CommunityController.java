package be.technifutur.gamenote.api.community;

import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.il.community.CommunityFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityFacade communityFacade;

    public CommunityController(CommunityFacade communityFacade) {
        this.communityFacade = communityFacade;
    }

    /** Classement des jeux par moyenne des notes de tous les joueurs du site. */
    @GetMapping("/games")
    public ResponseEntity<CommunityRankingDto> getRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(communityFacade.getRanking(page, size));
    }

    /** Fiche d'un jeu : moyenne, répartition des notes et fil des commentaires. */
    @GetMapping("/games/{igdbGameId}")
    public ResponseEntity<CommunityGameDetailDto> getGameDetail(Authentication authentication,
                                                                @PathVariable Long igdbGameId) {
        return ResponseEntity.ok(communityFacade.getGameDetail(authentication.getName(), igdbGameId));
    }
}
