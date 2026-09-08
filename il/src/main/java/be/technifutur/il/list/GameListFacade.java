package be.technifutur.il.list;

import be.technifutur.bll.list.GameListDetail;
import be.technifutur.bll.list.GameListService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.list.GameListEntity;
import be.technifutur.dal.list.GameListItemEntity;
import be.technifutur.dal.list.GameListSummaryView;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.list.AddListItemRequestDto;
import be.technifutur.dl.list.CreateListRequestDto;
import be.technifutur.dl.list.GameListDetailDto;
import be.technifutur.dl.list.GameListDto;
import be.technifutur.dl.list.GameListItemDto;
import be.technifutur.dl.list.GameListPageDto;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class GameListFacade {

    private final UserService userService;
    private final GameListService gameListService;
    private final GameListMapper gameListMapper;

    public GameListFacade(UserService userService,
                          GameListService gameListService,
                          GameListMapper gameListMapper) {
        this.userService = userService;
        this.gameListService = gameListService;
        this.gameListMapper = gameListMapper;
    }

    public GameListDto createList(String username, CreateListRequestDto request) {
        UserEntity me = userService.getByUsername(username);

        GameListEntity created = gameListService.createList(
                me.getId(),
                request.getName(),
                request.getDescription(),
                gameListMapper.toVisibility(request.getVisibility()));

        return summaryOf(created.getId(), me.getId());
    }

    public GameListDto updateList(String username, Long listId, CreateListRequestDto request) {
        UserEntity me = userService.getByUsername(username);

        gameListService.updateList(
                me.getId(),
                listId,
                request.getName(),
                request.getDescription(),
                gameListMapper.toVisibility(request.getVisibility()));

        return summaryOf(listId, me.getId());
    }

    public void deleteList(String username, Long listId) {
        gameListService.deleteList(userService.getByUsername(username).getId(), listId);
    }

    /** Les listes de l'utilisateur courant, publiques et privées. */
    public List<GameListDto> getMyLists(String username) {
        UserEntity me = userService.getByUsername(username);

        return toDtos(gameListService.getListsOf(me.getId(), me.getId()), me.getId());
    }

    /**
     * Les listes d'un membre, telles que peut les voir celui qui regarde : tout si c'est chez
     * lui, les publiques seulement sinon. C'est l'identité de qui appelle qui décide, jamais
     * l'écran d'où part l'appel.
     */
    public List<GameListDto> getListsOf(String username, Long ownerId) {
        UserEntity me = userService.getByUsername(username);

        return toDtos(gameListService.getListsOf(ownerId, me.getId()), me.getId());
    }

    /**
     * Les listes publiques d'un membre, sans qu'aucun compte soit nécessaire pour appeler.
     * Sert au profil public, qui est lui-même consultable par tout membre connecté.
     */
    public List<GameListDto> getPublicListsOf(Long ownerId, Long viewerId) {
        return toDtos(gameListService.getListsOf(ownerId, null), viewerId);
    }

    public GameListPageDto getPublicLists(String username, String search, int page, int size) {
        UserEntity me = userService.getByUsername(username);

        List<GameListSummaryView> lists = gameListService.getPublicLists(search, page, size);

        return new GameListPageDto(
                toDtos(lists, me.getId()),
                gameListService.countPublicLists(search),
                Math.max(page, 0),
                size);
    }

    public GameListDetailDto getList(String username, Long listId) {
        UserEntity me = userService.getByUsername(username);

        GameListDetail detail = gameListService.getList(listId, me.getId());

        List<GameListItemEntity> items = detail.items();
        List<GameListItemDto> itemDtos = new ArrayList<>(items.size());

        for (int index = 0; index < items.size(); index++) {
            itemDtos.add(gameListMapper.toItemDto(items.get(index), index));
        }

        // Les jaquettes de la vignette sont déjà dans le contenu qu'on vient de charger :
        // aller les redemander à la base pour les quatre premières serait une requête de plus
        // pour des lignes qu'on tient déjà.
        List<String> covers = items.stream()
                .map(GameListItemEntity::getCoverUrl)
                .filter(cover -> cover != null && !cover.isBlank())
                .limit(4)
                .toList();

        return new GameListDetailDto(
                gameListMapper.toDto(detail.summary(), covers, me.getId()),
                itemDtos);
    }

    public GameListDetailDto addItem(String username, Long listId, AddListItemRequestDto request) {
        UserEntity me = userService.getByUsername(username);

        gameListService.addItem(
                me.getId(),
                listId,
                request.getIgdbGameId(),
                request.getTitle() == null || request.getTitle().isBlank()
                        ? "Jeu #" + request.getIgdbGameId()
                        : request.getTitle(),
                request.getCoverUrl(),
                request.getReleaseDate(),
                request.getNote());

        return getList(username, listId);
    }

    public GameListDetailDto removeItem(String username, Long listId, Long igdbGameId) {
        gameListService.removeItem(userService.getByUsername(username).getId(), listId, igdbGameId);

        return getList(username, listId);
    }

    /**
     * Déplace un jeu à la place demandée.
     * <p>
     * Le rang arrive tel qu'il s'affiche, à partir de 1 ; le domaine compte à partir de 0. La
     * conversion se fait ici, avec celle du sens inverse dans le mappeur.
     */
    public GameListDetailDto moveItem(String username, Long listId, Long igdbGameId, int position) {
        gameListService.moveItem(userService.getByUsername(username).getId(), listId, igdbGameId, position - 1);

        return getList(username, listId);
    }

    /** Les listes de l'utilisateur qui contiennent déjà ce jeu. */
    public Set<Long> getListIdsContaining(String username, Long igdbGameId) {
        return gameListService.getListIdsContaining(userService.getByUsername(username).getId(), igdbGameId);
    }

    private GameListDto summaryOf(Long listId, Long viewerId) {
        GameListDetail detail = gameListService.getList(listId, viewerId);

        return gameListMapper.toDto(detail.summary(), gameListService.getCoverPreview(listId), viewerId);
    }

    /**
     * Les vignettes de tout l'index viennent d'une seule requête, et non d'un appel par liste
     * affichée : vingt listes coûtaient vingt allers-retours pour quatre adresses chacune.
     */
    private List<GameListDto> toDtos(List<GameListSummaryView> views, Long viewerId) {
        if (views.isEmpty()) {
            return List.of();
        }

        Map<Long, List<String>> covers = gameListService.getCoverPreviews(
                views.stream().map(GameListSummaryView::getId).toList());

        return views.stream()
                .map(view -> gameListMapper.toDto(view,
                        covers.getOrDefault(view.getId(), List.of()),
                        viewerId))
                .toList();
    }
}
