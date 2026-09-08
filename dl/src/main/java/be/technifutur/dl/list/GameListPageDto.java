package be.technifutur.dl.list;

import java.util.List;

/**
 * Une page de l'index des listes publiques.
 *
 * @param total nombre de listes correspondant à la recherche, toutes pages confondues
 */
public record GameListPageDto(List<GameListDto> lists, long total, int page, int size) {
}
