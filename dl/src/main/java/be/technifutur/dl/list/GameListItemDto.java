package be.technifutur.dl.list;

import java.time.OffsetDateTime;

/**
 * Un jeu dans une liste.
 *
 * @param position rang affiché, à partir de 1 — l'ordre est le propos d'un top, il doit se
 *                 lire tel quel sans que le front ait à recompter
 * @param note     mot de l'auteur sur ce jeu-là, propre à cette liste
 */
public record GameListItemDto(Long igdbGameId,
                              String title,
                              String coverUrl,
                              Long releaseDate,
                              String note,
                              int position,
                              OffsetDateTime addedAt) {
}
