package be.technifutur.dl.comment;

import java.time.OffsetDateTime;

/**
 * Commentaire tel qu'affiché dans le fil d'un jeu.
 *
 * @param authorRating note que l'auteur a mise à ce jeu, affichée à côté de son avis
 * @param mine         vrai si le commentaire appartient à l'utilisateur courant
 * @param canDelete    vrai si l'utilisateur courant peut le supprimer (le sien, ou n'importe
 *                     lequel s'il est administrateur) : évite au front de rejouer la règle
 */
public record GameCommentDto(Long id,
                             Long igdbGameId,
                             String content,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt,
                             Long authorId,
                             String authorUsername,
                             boolean authorHasAvatar,
                             Integer authorRating,
                             boolean mine,
                             boolean canDelete) {
}
