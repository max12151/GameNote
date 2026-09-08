package be.technifutur.dl.comment;

import java.util.List;

/**
 * Une page d'avis d'un joueur.
 *
 * @param total nombre total d'avis publiés par ce joueur, pour que le front sache s'il
 *              reste des pages à charger plutôt que de le déduire d'une page incomplète
 */
public record CommentPageDto(List<RecentCommentDto> comments,
                             long total,
                             int page,
                             int size) {
}
