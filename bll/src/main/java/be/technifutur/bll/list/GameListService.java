package be.technifutur.bll.list;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import be.technifutur.dal.list.GameListEntity;
import be.technifutur.dal.list.GameListItemEntity;
import be.technifutur.dal.list.GameListItemRepository;
import be.technifutur.dal.list.GameListRepository;
import be.technifutur.dal.list.GameListSummaryView;
import be.technifutur.dal.list.ListCoverView;
import be.technifutur.dal.list.ListVisibility;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Les listes personnalisées : « Mon top 10 2026 », « Les meilleurs metroidvanias ».
 * <p>
 * Une liste appartient à un membre, choisit d'être publique ou privée, et garde l'ordre que
 * son auteur lui donne — dans un top, la place compte autant que la présence.
 */
@Service
public class GameListService {

    /** Nombre de listes qu'un membre peut créer. Assez pour ranger, trop peu pour spammer. */
    private static final int MAX_LISTS_PER_USER = 50;

    /** Nombre de jeux par liste. */
    private static final int MAX_ITEMS_PER_LIST = 200;

    private static final int MAX_PAGE_SIZE = 50;

    /** Jaquettes ramenées pour la vignette d'un index de listes. */
    private static final int COVER_PREVIEW = 4;

    private final GameListRepository gameListRepository;
    private final GameListItemRepository gameListItemRepository;

    public GameListService(GameListRepository gameListRepository,
                           GameListItemRepository gameListItemRepository) {
        this.gameListRepository = gameListRepository;
        this.gameListItemRepository = gameListItemRepository;
    }

    @Transactional
    public GameListEntity createList(Long userId, String name, String description, ListVisibility visibility) {
        if (gameListRepository.countByUserId(userId) >= MAX_LISTS_PER_USER) {
            throw new InvalidOperationException("Vous avez atteint le nombre maximum de listes (" + MAX_LISTS_PER_USER + ")");
        }

        GameListEntity list = new GameListEntity();
        list.setUserId(userId);
        list.setName(name.strip());
        list.setDescription(blankToNull(description));
        list.setVisibility(visibility == null ? ListVisibility.PRIVATE : visibility);
        list.setCreatedAt(OffsetDateTime.now());

        return gameListRepository.save(list);
    }

    @Transactional
    public GameListEntity updateList(Long userId, Long listId, String name, String description,
                                     ListVisibility visibility) {
        GameListEntity list = requireOwnedList(userId, listId);

        list.setName(name.strip());
        list.setDescription(blankToNull(description));

        if (visibility != null) {
            list.setVisibility(visibility);
        }

        return gameListRepository.save(list);
    }

    @Transactional
    public void deleteList(Long userId, Long listId) {
        GameListEntity list = requireOwnedList(userId, listId);

        gameListItemRepository.deleteByGameListId(list.getId());
        gameListRepository.delete(list);
    }

    /**
     * Les listes d'un membre, vues par {@code viewerId}.
     * <p>
     * Le propriétaire voit les siennes en entier, un visiteur seulement les publiques. Le
     * même appel sert les deux cas : c'est l'identité de qui regarde qui décide, pas
     * l'écran d'où part l'appel.
     */
    public List<GameListSummaryView> getListsOf(Long ownerId, Long viewerId) {
        boolean owner = ownerId.equals(viewerId);

        return gameListRepository.findByOwner(ownerId, owner, PageRequest.of(0, MAX_LISTS_PER_USER));
    }

    public List<GameListSummaryView> getPublicLists(String search, int page, int size) {
        String safeSearch = search == null ? "" : search.strip();

        return gameListRepository.findPublicLists(safeSearch,
                PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE)));
    }

    public long countPublicLists(String search) {
        return gameListRepository.countPublicLists(search == null ? "" : search.strip());
    }

    /**
     * Une liste et son contenu.
     * <p>
     * Une liste privée consultée par quelqu'un d'autre répond « introuvable » et non
     * « interdit » : distinguer les deux apprendrait à un curieux qu'une liste existe bien à
     * cet identifiant.
     */
    public GameListDetail getList(Long listId, Long viewerId) {
        GameListSummaryView summary = gameListRepository.findSummaryById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Liste introuvable"));

        if (summary.getVisibility() == ListVisibility.PRIVATE && !summary.getOwnerId().equals(viewerId)) {
            throw new ResourceNotFoundException("Liste introuvable");
        }

        return new GameListDetail(summary, gameListItemRepository.findByGameListIdOrderBySortIndexAsc(listId));
    }

    /** Les premières jaquettes d'une liste, pour sa vignette dans un index. */
    public List<String> getCoverPreview(Long listId) {
        return gameListItemRepository.findCovers(listId, PageRequest.of(0, COVER_PREVIEW));
    }

    /**
     * Les vignettes de tout un index en une requête, plutôt qu'une par liste affichée : c'est
     * le même geste que le comptage des commentaires sur une page de classement.
     * <p>
     * Une liste sans jaquette est absente de la carte : c'est à l'appelant de lire une liste
     * vide.
     */
    public Map<Long, List<String>> getCoverPreviews(Collection<Long> listIds) {
        if (listIds.isEmpty()) {
            return Map.of();
        }

        return gameListItemRepository.findCoverPreviews(listIds, COVER_PREVIEW).stream()
                .collect(Collectors.groupingBy(ListCoverView::getGameListId,
                        Collectors.mapping(ListCoverView::getCoverUrl, Collectors.toList())));
    }

    /**
     * Dans quelles listes de ce membre ce jeu figure-t-il déjà ? Sert à cocher les bonnes
     * cases dans le menu « ajouter à une liste » de la fiche d'un jeu.
     */
    public Set<Long> getListIdsContaining(Long userId, Long igdbGameId) {
        return Set.copyOf(gameListItemRepository.findListIdsContaining(userId, igdbGameId));
    }

    /**
     * Ajoute un jeu à la fin de la liste.
     * <p>
     * Le rang est celui qui suit le dernier utilisé, et non le nombre d'éléments : après un
     * retrait suivi d'un ajout, compter les éléments donnerait un rang déjà pris.
     */
    @Transactional
    public GameListItemEntity addItem(Long userId, Long listId, Long igdbGameId, String title,
                                      String coverUrl, Long releaseDate, String note) {
        requireOwnedList(userId, listId);

        if (gameListItemRepository.existsByGameListIdAndIgdbGameId(listId, igdbGameId)) {
            throw new DuplicateResourceException("Ce jeu est déjà dans la liste");
        }

        if (gameListItemRepository.countByGameListId(listId) >= MAX_ITEMS_PER_LIST) {
            throw new InvalidOperationException("Cette liste a atteint sa taille maximum (" + MAX_ITEMS_PER_LIST + " jeux)");
        }

        Integer lastIndex = gameListItemRepository.findMaxSortIndex(listId);

        GameListItemEntity item = new GameListItemEntity();
        item.setGameListId(listId);
        item.setIgdbGameId(igdbGameId);
        item.setTitle(title);
        item.setCoverUrl(coverUrl);
        item.setReleaseDate(releaseDate);
        item.setNote(blankToNull(note));
        item.setSortIndex(lastIndex == null ? 0 : lastIndex + 1);
        item.setAddedAt(OffsetDateTime.now());

        touch(listId);

        return gameListItemRepository.save(item);
    }

    @Transactional
    public void removeItem(Long userId, Long listId, Long igdbGameId) {
        requireOwnedList(userId, listId);

        GameListItemEntity item = gameListItemRepository.findByGameListIdAndIgdbGameId(listId, igdbGameId)
                .orElseThrow(() -> new ResourceNotFoundException("Ce jeu n'est pas dans la liste"));

        gameListItemRepository.delete(item);
        renumber(listId, item.getId());
        touch(listId);
    }

    /**
     * Déplace un jeu à la place demandée, les autres se décalant autour de lui.
     * <p>
     * La liste entière est renumérotée à la suite : c'est le seul moyen simple de garantir des
     * rangs contigus et sans doublon après n'importe quelle suite de déplacements. Une liste
     * plafonne à {@value #MAX_ITEMS_PER_LIST} jeux, l'opération reste sans conséquence.
     */
    @Transactional
    public void moveItem(Long userId, Long listId, Long igdbGameId, int targetIndex) {
        requireOwnedList(userId, listId);

        List<GameListItemEntity> items =
                new ArrayList<>(gameListItemRepository.findByGameListIdOrderBySortIndexAsc(listId));

        int currentIndex = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getIgdbGameId().equals(igdbGameId)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            throw new ResourceNotFoundException("Ce jeu n'est pas dans la liste");
        }

        int destination = Math.clamp(targetIndex, 0, items.size() - 1);

        if (destination == currentIndex) {
            return;
        }

        items.add(destination, items.remove(currentIndex));

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortIndex(i);
        }

        gameListItemRepository.saveAll(items);
        touch(listId);
    }

    /** Referme les trous laissés par un retrait, pour que les rangs restent contigus. */
    private void renumber(Long listId, Long removedItemId) {
        List<GameListItemEntity> items = gameListItemRepository.findByGameListIdOrderBySortIndexAsc(listId).stream()
                .filter(item -> !item.getId().equals(removedItemId))
                .toList();

        for (int i = 0; i < items.size(); i++) {
            items.get(i).setSortIndex(i);
        }

        gameListItemRepository.saveAll(items);
    }

    /**
     * Marque la liste comme modifiée quand c'est son contenu qui bouge.
     * <p>
     * Sans cela, ajouter dix jeux à une liste la laisserait au fond de l'index « mes listes »,
     * trié sur la dernière modification : JPA ne voit pas passer une écriture faite dans une
     * autre table.
     */
    private void touch(Long listId) {
        gameListRepository.findById(listId).ifPresent(list -> {
            list.setUpdatedAt(OffsetDateTime.now());
            gameListRepository.save(list);
        });
    }

    private GameListEntity requireOwnedList(Long userId, Long listId) {
        GameListEntity list = gameListRepository.findById(listId)
                .orElseThrow(() -> new ResourceNotFoundException("Liste introuvable"));

        if (!list.getUserId().equals(userId)) {
            throw new ForbiddenOperationException("Cette liste ne vous appartient pas");
        }

        return list;
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
