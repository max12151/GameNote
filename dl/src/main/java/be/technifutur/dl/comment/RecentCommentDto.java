package be.technifutur.dl.comment;

import java.time.OffsetDateTime;

/**
 * Commentaire présenté hors de la fiche d'un jeu : flux des derniers avis de l'accueil,
 * ou avis récents d'un joueur sur son profil.
 * <p>
 * Volontairement dépourvu des drapeaux {@code mine} / {@code canDelete} de
 * {@link GameCommentDto} : on ne modère pas depuis ces listes, on y renvoie vers la fiche
 * du jeu. Y transporter des droits qu'aucun bouton n'utilise reviendrait à publier une
 * information sur l'utilisateur courant sans raison.
 */
public record RecentCommentDto(Long id,
                               Long igdbGameId,
                               String gameTitle,
                               String gameCoverUrl,
                               String content,
                               OffsetDateTime createdAt,
                               Long authorId,
                               String authorUsername,
                               boolean authorHasAvatar,
                               Integer authorRating) {
}
