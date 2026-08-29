package be.technifutur.dal.rating;

/** Nombre de joueurs ayant attribué une note donnée à un jeu (histogramme 1→10). */
public interface RatingBucketView {

    int getRating();

    long getCount();
}
