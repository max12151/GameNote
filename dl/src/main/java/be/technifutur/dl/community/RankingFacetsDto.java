package be.technifutur.dl.community;

import java.util.List;

/**
 * Les valeurs proposées dans les filtres du classement.
 * <p>
 * Servies par une route à part plutôt qu'avec chaque page de classement : elles ne changent
 * qu'au rythme des jeux notés sur le site, et les renvoyer à chaque changement de page ferait
 * repayer une liste identique.
 */
public record RankingFacetsDto(List<String> genres, List<String> platforms) {
}
