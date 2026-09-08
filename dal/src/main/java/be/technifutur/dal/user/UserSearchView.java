package be.technifutur.dal.user;

/**
 * Ligne de résultat d'une recherche de membre.
 * <p>
 * L'avatar n'est représenté que par un booléen : l'image est une data URI base64 pouvant
 * peser deux mégaoctets, et une recherche en ramène plusieurs d'un coup. Le front la
 * demande à la route dédiée, qui la sert avec un ETag et laisse le navigateur la cacher.
 */
public interface UserSearchView {

    Long getId();

    String getUsername();

    boolean getHasAvatar();
}
