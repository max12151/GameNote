package be.technifutur.dl.list;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Une liste telle qu'elle apparaît dans un index, sans son contenu.
 *
 * @param coverPreview quelques jaquettes en guise de vignette. Ramener les deux cents jeux
 *                     d'une liste pour en dessiner l'aperçu ferait payer à chaque index une
 *                     réponse que personne ne lit.
 * @param mine         vrai si la liste appartient à l'utilisateur courant : c'est ce qui
 *                     décide de l'affichage des boutons de modification
 */
public record GameListDto(Long id,
                          String name,
                          String description,
                          ListVisibilityDto visibility,
                          long itemCount,
                          List<String> coverPreview,
                          Long ownerId,
                          String ownerUsername,
                          boolean mine,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {
}
