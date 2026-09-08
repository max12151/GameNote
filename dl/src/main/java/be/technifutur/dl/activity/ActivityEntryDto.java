package be.technifutur.dl.activity;

import java.time.OffsetDateTime;

/**
 * Une ligne du fil d'activité.
 *
 * @param content     texte de l'avis, nul pour une note
 * @param commentId   identifiant de l'avis, nul pour une note : le front en a besoin pour
 *                    proposer de le marquer comme utile depuis le fil
 * @param markedUseful vrai si l'utilisateur courant a déjà marqué cet avis
 */
public record ActivityEntryDto(ActivityKindDto kind,
                               OffsetDateTime at,
                               Long authorId,
                               String authorUsername,
                               boolean authorHasAvatar,
                               Long igdbGameId,
                               String gameTitle,
                               String gameCoverUrl,
                               Integer rating,
                               String content,
                               Long commentId,
                               long usefulCount,
                               boolean markedUseful) {
}
