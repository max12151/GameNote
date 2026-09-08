package be.technifutur.dal.rating;

/** Nombre de jeux notés par un utilisateur, agrégé en une requête pour toute une liste. */
public interface UserRatingCountView {

    Long getUserId();

    long getRatedGames();
}
