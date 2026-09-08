package be.technifutur.gamenote.api.list;

import be.technifutur.dl.list.AddListItemRequestDto;
import be.technifutur.dl.list.CreateListRequestDto;
import be.technifutur.dl.list.GameListDetailDto;
import be.technifutur.dl.list.GameListDto;
import be.technifutur.dl.list.GameListPageDto;
import be.technifutur.dl.list.MoveListItemRequestDto;
import be.technifutur.il.list.GameListFacade;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Les listes personnalisées.
 * <p>
 * Les opérations sur le contenu renvoient la liste entière plutôt qu'un accusé de réception :
 * ajouter, retirer ou déplacer un jeu renumérote les rangs, et le front doit repartir de
 * l'ordre réel plutôt que d'essayer de le rejouer de son côté.
 */
@RestController
@RequestMapping("/api/lists")
public class GameListController {

    private final GameListFacade gameListFacade;

    public GameListController(GameListFacade gameListFacade) {
        this.gameListFacade = gameListFacade;
    }

    /** Les listes de l'utilisateur courant, publiques et privées. */
    @GetMapping("/mine")
    public ResponseEntity<List<GameListDto>> getMyLists(Authentication authentication) {
        return ResponseEntity.ok(gameListFacade.getMyLists(authentication.getName()));
    }

    /**
     * Les listes d'un autre membre. Seules les publiques sortent, sauf si l'appelant est ce
     * membre : la règle est tenue côté serveur, pas par la route employée.
     */
    @GetMapping("/players/{userId}")
    public ResponseEntity<List<GameListDto>> getPlayerLists(Authentication authentication,
                                                            @PathVariable Long userId) {
        return ResponseEntity.ok(gameListFacade.getListsOf(authentication.getName(), userId));
    }

    /** L'index des listes publiques du site. */
    @GetMapping
    public ResponseEntity<GameListPageDto> getPublicLists(Authentication authentication,
                                                          @RequestParam(required = false) String search,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(gameListFacade.getPublicLists(authentication.getName(), search, page, size));
    }

    @GetMapping("/{listId}")
    public ResponseEntity<GameListDetailDto> getList(Authentication authentication, @PathVariable Long listId) {
        return ResponseEntity.ok(gameListFacade.getList(authentication.getName(), listId));
    }

    /**
     * Celles de mes listes qui contiennent déjà ce jeu.
     * <p>
     * La fiche communautaire transporte déjà l'information, mais elle n'est pas le seul
     * endroit d'où l'on ajoute un jeu à une liste : la collection et la recherche ouvrent
     * la même boîte. Leur faire charger une fiche entière pour n'en lire qu'un tableau
     * d'identifiants aurait coûté une requête agrégée et tout un fil d'avis.
     */
    @GetMapping("/containing/{igdbGameId}")
    public ResponseEntity<List<Long>> getListsContaining(Authentication authentication,
                                                         @PathVariable Long igdbGameId) {
        return ResponseEntity.ok(
                List.copyOf(gameListFacade.getListIdsContaining(authentication.getName(), igdbGameId)));
    }

    @PostMapping
    public ResponseEntity<GameListDto> createList(Authentication authentication,
                                                  @Valid @RequestBody CreateListRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gameListFacade.createList(authentication.getName(), request));
    }

    @PutMapping("/{listId}")
    public ResponseEntity<GameListDto> updateList(Authentication authentication,
                                                  @PathVariable Long listId,
                                                  @Valid @RequestBody CreateListRequestDto request) {
        return ResponseEntity.ok(gameListFacade.updateList(authentication.getName(), listId, request));
    }

    @DeleteMapping("/{listId}")
    public ResponseEntity<Void> deleteList(Authentication authentication, @PathVariable Long listId) {
        gameListFacade.deleteList(authentication.getName(), listId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{listId}/games")
    public ResponseEntity<GameListDetailDto> addGame(Authentication authentication,
                                                     @PathVariable Long listId,
                                                     @Valid @RequestBody AddListItemRequestDto request) {
        return ResponseEntity.ok(gameListFacade.addItem(authentication.getName(), listId, request));
    }

    @DeleteMapping("/{listId}/games/{igdbGameId}")
    public ResponseEntity<GameListDetailDto> removeGame(Authentication authentication,
                                                        @PathVariable Long listId,
                                                        @PathVariable Long igdbGameId) {
        return ResponseEntity.ok(gameListFacade.removeItem(authentication.getName(), listId, igdbGameId));
    }

    /** Déplace un jeu à la place demandée, comptée à partir de 1 comme à l'écran. */
    @PutMapping("/{listId}/games/{igdbGameId}/position")
    public ResponseEntity<GameListDetailDto> moveGame(Authentication authentication,
                                                      @PathVariable Long listId,
                                                      @PathVariable Long igdbGameId,
                                                      @Valid @RequestBody MoveListItemRequestDto request) {
        return ResponseEntity.ok(gameListFacade.moveItem(
                authentication.getName(), listId, igdbGameId, request.getPosition()));
    }
}
