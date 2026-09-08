package be.technifutur.dl.activity;

import java.util.List;

/**
 * Une page du fil d'activité.
 *
 * @param following nombre de joueurs suivis : un fil vide n'a pas la même signification selon
 *                  qu'on ne suit personne ou que ceux qu'on suit n'ont rien publié, et le
 *                  front doit pouvoir le dire
 * @param hasMore   vrai s'il reste des entrées à charger. Le total exact n'est pas calculé :
 *                  il faudrait compter deux tables et fusionner, pour un chiffre que personne
 *                  ne lit dans un fil.
 */
public record ActivityFeedDto(List<ActivityEntryDto> entries,
                              int page,
                              int size,
                              boolean hasMore,
                              long following) {
}
