package be.technifutur.dal.rating;

/**
 * Ligne du classement communautaire : un jeu, sa moyenne et son nombre de votes,
 * agrégés sur les notes de tous les utilisateurs du site.
 * <p>
 * Le titre, la jaquette et la date de sortie sont pris via {@code max(...)} : ils sont
 * identiques sur toutes les lignes d'un même jeu (recopiés depuis IGDB au moment de la
 * notation), l'agrégat sert donc uniquement à satisfaire le GROUP BY.
 */
public interface CommunityGameView {

    Long getIgdbGameId();

    String getTitle();

    String getCoverUrl();

    Long getReleaseDate();

    Double getAverageRating();

    long getRatingCount();
}
