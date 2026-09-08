package be.technifutur.bll.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.dal.list.GameListEntity;
import be.technifutur.dal.list.GameListItemEntity;
import be.technifutur.dal.list.GameListItemRepository;
import be.technifutur.dal.list.GameListRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * L'ordre d'une liste, et à qui elle appartient.
 * <p>
 * Dans un top, la place compte autant que la présence : une renumérotation ratée ne lève
 * aucune erreur, elle réordonne le classement de quelqu'un sans qu'il l'ait demandé.
 */
class GameListServiceTest {

    private static final long OWNER = 7L;
    private static final long LIST = 42L;

    private GameListRepository listRepository;
    private GameListItemRepository itemRepository;
    private GameListService service;

    @BeforeEach
    void setUp() {
        listRepository = mock(GameListRepository.class);
        itemRepository = mock(GameListItemRepository.class);
        service = new GameListService(listRepository, itemRepository);

        when(listRepository.findById(LIST)).thenReturn(Optional.of(list(OWNER)));
    }

    @Test
    @DisplayName("Déplacer un jeu renumérote toute la liste sans trou ni doublon")
    void moveRenumbersEveryItem() {
        when(itemRepository.findByGameListIdOrderBySortIndexAsc(LIST))
                .thenReturn(List.of(item(100L, 0), item(200L, 1), item(300L, 2), item(400L, 3)));

        // Le troisième jeu passe en tête.
        service.moveItem(OWNER, LIST, 300L, 0);

        List<GameListItemEntity> saved = captureSaved();

        assertEquals(List.of(300L, 100L, 200L, 400L),
                saved.stream().map(GameListItemEntity::getIgdbGameId).toList());
        assertEquals(List.of(0, 1, 2, 3),
                saved.stream().map(GameListItemEntity::getSortIndex).toList());
    }

    @Test
    @DisplayName("Une place hors bornes est ramenée à la fin plutôt que refusée")
    void clampsDestination() {
        when(itemRepository.findByGameListIdOrderBySortIndexAsc(LIST))
                .thenReturn(List.of(item(100L, 0), item(200L, 1), item(300L, 2)));

        service.moveItem(OWNER, LIST, 100L, 99);

        assertEquals(List.of(200L, 300L, 100L),
                captureSaved().stream().map(GameListItemEntity::getIgdbGameId).toList());
    }

    @Test
    @DisplayName("Déplacer un jeu à la place qu'il occupe déjà n'écrit rien")
    void movingToSamePositionIsANoOp() {
        when(itemRepository.findByGameListIdOrderBySortIndexAsc(LIST))
                .thenReturn(List.of(item(100L, 0), item(200L, 1)));

        service.moveItem(OWNER, LIST, 200L, 1);

        verify(itemRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Retirer un jeu referme le trou laissé dans les rangs")
    void removeClosesTheGap() {
        GameListItemEntity removed = item(200L, 1);

        when(itemRepository.findByGameListIdAndIgdbGameId(LIST, 200L)).thenReturn(Optional.of(removed));
        when(itemRepository.findByGameListIdOrderBySortIndexAsc(LIST))
                .thenReturn(List.of(item(100L, 0), removed, item(300L, 2)));

        service.removeItem(OWNER, LIST, 200L);

        verify(itemRepository).delete(removed);
        assertEquals(List.of(0, 1),
                captureSaved().stream().map(GameListItemEntity::getSortIndex).toList());
    }

    @Test
    @DisplayName("La liste d'un autre membre est intouchable")
    void refusesForeignList() {
        assertThrows(ForbiddenOperationException.class,
                () -> service.moveItem(OWNER + 1, LIST, 100L, 0));
        assertThrows(ForbiddenOperationException.class,
                () -> service.deleteList(OWNER + 1, LIST));
    }

    @Test
    @DisplayName("Un jeu déjà présent n'est pas ajouté une seconde fois")
    void refusesDuplicateGame() {
        when(itemRepository.existsByGameListIdAndIgdbGameId(LIST, 100L)).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> service.addItem(OWNER, LIST, 100L, "Hollow Knight", null, null, null));
    }

    @Test
    @DisplayName("Le rang d'un ajout suit le dernier utilisé, pas le nombre d'éléments")
    void appendsAfterHighestIndex() {
        // Après un retrait, compter les éléments donnerait un rang déjà pris.
        when(itemRepository.countByGameListId(LIST)).thenReturn(2L);
        when(itemRepository.findMaxSortIndex(LIST)).thenReturn(5);
        when(itemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        GameListItemEntity added = service.addItem(OWNER, LIST, 100L, "Celeste", null, null, null);

        assertEquals(6, added.getSortIndex());
    }

    @Test
    @DisplayName("Le premier jeu d'une liste vide prend le rang zéro")
    void firstItemStartsAtZero() {
        when(itemRepository.findMaxSortIndex(LIST)).thenReturn(null);
        when(itemRepository.save(any())).thenAnswer(call -> call.getArgument(0));

        assertEquals(0, service.addItem(OWNER, LIST, 100L, "Celeste", null, null, null).getSortIndex());
    }

    @SuppressWarnings("unchecked")
    private List<GameListItemEntity> captureSaved() {
        ArgumentCaptor<List<GameListItemEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(itemRepository).saveAll(captor.capture());

        return captor.getValue();
    }

    private static GameListEntity list(long ownerId) {
        GameListEntity list = new GameListEntity();
        list.setId(LIST);
        list.setUserId(ownerId);
        list.setName("Mon top");
        list.setCreatedAt(OffsetDateTime.now());
        return list;
    }

    private static GameListItemEntity item(long igdbGameId, int sortIndex) {
        GameListItemEntity item = new GameListItemEntity();
        item.setId(igdbGameId);
        item.setGameListId(LIST);
        item.setIgdbGameId(igdbGameId);
        item.setTitle("Jeu " + igdbGameId);
        item.setSortIndex(sortIndex);
        item.setAddedAt(OffsetDateTime.now());
        return item;
    }
}
