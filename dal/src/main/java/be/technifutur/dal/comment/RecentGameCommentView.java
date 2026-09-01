package be.technifutur.dal.comment;

/**
 * Commentaire lu en dehors du fil d'un jeu : le flux des derniers avis du site, ou les
 * avis d'un joueur donné. Dans ces deux cas le jeu n'est plus le contexte mais une
 * information à afficher, d'où le titre et la jaquette en plus.
 * <p>
 * Les deux viennent de la note de l'auteur, pas d'une table de jeux : le site n'en a pas.
 * La jointure est sûre — commenter exige d'avoir noté, et retirer sa note supprime le
 * commentaire, donc la ligne de note existe toujours tant que le commentaire existe.
 */
public interface RecentGameCommentView extends GameCommentView {

    String getGameTitle();

    String getGameCoverUrl();
}
