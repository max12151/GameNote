package be.technifutur.dl.comment;

import java.time.OffsetDateTime;

/**
 * Commentaire tel qu'affiché dans le fil d'un jeu.
 *
 * @param authorRating note que l'auteur a mise à ce jeu, affichée à côté de son avis
 * @param mine         vrai si le commentaire appartient à l'utilisateur courant
 * @param canDelete    vrai si l'utilisateur courant peut le supprimer (le sien, ou n'importe
 *                     lequel s'il est administrateur) : évite au front de rejouer la règle
 * @param authorDeleted vrai si l'auteur a depuis supprimé son compte. Son avis reste lisible
 *                      — le fil ne doit pas se retrouver troué — mais son pseudo ne mène plus
 *                      à un profil.
 * @param usefulCount  combien de membres ont trouvé cet avis utile
 * @param markedUseful vrai si l'utilisateur courant fait partie de ceux-là
 * @param canReact     faux sur son propre avis : se distinguer soi-même n'apprendrait rien
 *                     aux autres lecteurs
 */
public record GameCommentDto(Long id,
                             Long igdbGameId,
                             String content,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt,
                             Long authorId,
                             String authorUsername,
                             boolean authorHasAvatar,
                             boolean authorDeleted,
                             Integer authorRating,
                             boolean mine,
                             boolean canDelete,
                             long usefulCount,
                             boolean markedUseful,
                             boolean canReact) {
}
