package be.technifutur.gamenote.api.community;

import be.technifutur.dl.comment.RecentCommentDto;
import be.technifutur.dl.community.CommunityGameDetailDto;
import be.technifutur.dl.community.CommunityGameInfoDto;
import be.technifutur.dl.community.CommunityRankingDto;
import be.technifutur.gamenote.api.igdb.IgdbGameClient;
import be.technifutur.gamenote.api.igdb.IgdbGameDto;
import be.technifutur.il.community.CommunityFacade;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
@RequestMapping("/api/community")
public class CommunityController {

    private static final Logger log = LoggerFactory.getLogger(CommunityController.class);

    private final CommunityFacade communityFacade;
    private final IgdbGameClient igdbGameClient;

    public CommunityController(CommunityFacade communityFacade, IgdbGameClient igdbGameClient) {
        this.communityFacade = communityFacade;
        this.igdbGameClient = igdbGameClient;
    }

    /**
     * Classement des jeux par moyenne des notes de tous les joueurs du site.
     *
     * @param search filtre optionnel sur le titre, appliqué par la base afin qu'un jeu classé
     *               au-delà de la première page reste trouvable
     */
    @GetMapping("/games")
    public ResponseEntity<CommunityRankingDto> getRanking(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(communityFacade.getRanking(page, size, search));
    }

    /**
     * Derniers avis publiés sur le site, tous jeux confondus : de quoi montrer sur la page
     * d'accueil que le site est vivant, et donner une porte d'entrée vers les fiches.
     * <p>
     * Route authentifiée, contrairement au classement juste au-dessus : celui-ci est une
     * moyenne, une donnée agrégée où personne n'est reconnaissable, alors qu'un avis porte
     * le pseudo, la photo et les mots de quelqu'un.
     */
    @GetMapping("/comments/recent")
    public ResponseEntity<List<RecentCommentDto>> getRecentComments(
            @RequestParam(defaultValue = "6") int limit
    ) {
        return ResponseEntity.ok(communityFacade.getRecentComments(limit));
    }

    /**
     * Fiche d'un jeu : moyenne, répartition des notes et fil des commentaires.
     * <p>
     * Un jeu que personne n'a encore noté n'est pas une erreur : on le présente alors avec
     * ses métadonnées IGDB et un classement vide, pour que l'utilisateur puisse être le
     * premier à le noter. Seul un identifiant inconnu d'IGDB donne un 404.
     */
    @GetMapping("/games/{igdbGameId}")
    public ResponseEntity<CommunityGameDetailDto> getGameDetail(Authentication authentication,
                                                                @PathVariable Long igdbGameId) {
        return communityFacade.findGameDetail(authentication.getName(), igdbGameId)
                .or(() -> fetchFromIgdb(igdbGameId))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Repli sur IGDB pour un jeu dont le site n'a aucune note.
     * <p>
     * IGDB peut être injoignable — réseau filtré, quota atteint, panne. Ce n'est pas une
     * raison de renvoyer une erreur serveur : la seule information réellement absente est
     * le descriptif d'un jeu que personne n'a noté. On dégrade donc en « introuvable »,
     * que le front sait déjà présenter.
     */
    private Optional<CommunityGameDetailDto> fetchFromIgdb(Long igdbGameId) {
        try {
            return igdbGameClient.fetchById(igdbGameId)
                    .map(IgdbGameDto::from)
                    .map(CommunityController::emptyDetail);
        } catch (RestClientException igdbIndisponible) {
            log.warn("IGDB injoignable pour le jeu {} : fiche communautaire servie comme introuvable",
                    igdbGameId, igdbIndisponible);
            return Optional.empty();
        }
    }

    /** Fiche d'un jeu connu d'IGDB mais que personne n'a encore noté sur le site. */
    private static CommunityGameDetailDto emptyDetail(IgdbGameDto game) {
        CommunityGameInfoDto info = new CommunityGameInfoDto(
                game.igdbId(),
                game.title(),
                game.coverUrl(),
                game.firstReleaseDate(),
                game.summary(),
                game.genres(),
                game.developers(),
                game.publishers(),
                game.platforms()
        );

        return new CommunityGameDetailDto(info, 0, 0, List.of(), List.of(), null, null);
    }
}
