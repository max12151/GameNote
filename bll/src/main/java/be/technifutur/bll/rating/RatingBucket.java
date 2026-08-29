package be.technifutur.bll.rating;

/** Nombre de jeux auxquels l'utilisateur a attribue la note {@code rating}. */
public record RatingBucket(int rating, long count) {
}
