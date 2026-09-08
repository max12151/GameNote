package be.technifutur.bll.rating;

import java.util.List;

/**
 * Ce que le client sait d'un jeu au moment où il le range dans sa bibliothèque.
 * <p>
 * Ces informations viennent d'IGDB et sont recopiées à chaque enregistrement : le site n'a
 * pas de table de jeux, et une fiche doit rester affichable même quand IGDB est injoignable.
 * <p>
 * Un enregistrement plutôt que neuf paramètres alignés : la méthode de notation en comptait
 * déjà onze, où deux listes voisines de chaînes s'échangeaient sans que le compilateur y
 * trouve rien à redire.
 *
 * @param releaseDate date de sortie en secondes Unix, telle qu'IGDB la donne
 */
public record GameMetadata(Long igdbGameId,
                           String title,
                           String coverUrl,
                           Long releaseDate,
                           String summary,
                           List<String> genres,
                           List<String> developers,
                           List<String> publishers,
                           List<String> platforms) {
}
