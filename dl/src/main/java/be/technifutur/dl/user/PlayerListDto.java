package be.technifutur.dl.user;

import java.util.List;

/**
 * Une liste de membres — abonnés, abonnements — avec son total.
 *
 * @param total nombre réel de membres, qui peut dépasser ce que la page contient
 */
public record PlayerListDto(List<UserSearchResultDto> players, long total) {
}
