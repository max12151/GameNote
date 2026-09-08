package be.technifutur.dl.user;

/**
 * Les compteurs sociaux d'un profil.
 *
 * @param following      nombre de joueurs que ce membre suit
 * @param followers      nombre de membres qui le suivent
 * @param followedByMe   vrai si l'utilisateur courant le suit déjà ; nul n'a de sens sur son
 *                       propre profil, où le bouton n'est pas proposé
 * @param isMe           vrai lorsque le profil consulté est celui de l'utilisateur courant
 */
public record FollowStatsDto(long following,
                             long followers,
                             boolean followedByMe,
                             boolean isMe) {
}
