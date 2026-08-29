package be.technifutur.dal.comment;

/** Nombre de commentaires d'un jeu, agrégé en une requête pour une liste de jeux. */
public interface GameCommentCountView {

    Long getIgdbGameId();

    long getCommentCount();
}
