package be.technifutur.dal.rating;

/** Nombre d'entrées de bibliothèque d'un joueur pour un statut donné. */
public interface StatusCountView {

    GameStatus getStatus();

    long getCount();
}
