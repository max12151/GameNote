package be.technifutur.dl.user;

/**
 * Membre trouvé par une recherche de pseudo.
 * <p>
 * Volontairement pauvre : de quoi reconnaître quelqu'un et décider d'ouvrir son profil,
 * rien de plus. Ni e-mail, ni bio, ni date d'inscription — ces informations appartiennent
 * à la fiche du joueur, pas à une liste de résultats.
 *
 * @param hasAvatar dit à la pastille d'identité s'il y a une image à demander à la route
 *                  dédiée, ou s'il faut se rabattre sur les initiales
 * @param ratedGames nombre de jeux notés : le seul chiffre qui aide à choisir entre deux
 *                   pseudos proches, en distinguant un membre actif d'un compte dormant
 * @param followedByMe vrai si l'utilisateur courant suit déjà ce membre. Le drapeau vient
 *                     d'une requête unique pour toute la liste, ce qui permet d'afficher le
 *                     bon bouton sans un aller-retour par ligne.
 */
public record UserSearchResultDto(Long id,
                                  String username,
                                  boolean hasAvatar,
                                  long ratedGames,
                                  boolean followedByMe) {
}
