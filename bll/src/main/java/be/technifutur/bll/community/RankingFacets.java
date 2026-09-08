package be.technifutur.bll.community;

import java.util.List;

/**
 * Les valeurs proposées dans les filtres du classement.
 * <p>
 * Elles viennent des notes existantes et non d'une liste écrite à la main : proposer un genre
 * que personne n'a noté mène à une page vide, et en oublier un rend une partie du catalogue
 * infiltrable.
 */
public record RankingFacets(List<String> genres, List<String> platforms) {
}
