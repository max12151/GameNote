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
 * <p>
 * Le compteur « utile », lui, y figure : il se lit sans rien pouvoir en faire, comme la note.
 *
 * @param authorDeleted vrai si l'auteur a depuis supprimé son compte : son pseudo reste
 *                      affiché mais ne mène plus à un profil
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
                               boolean authorDeleted,
                               Integer authorRating,
                               long usefulCount) {
}
