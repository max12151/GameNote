package be.technifutur.dl.rating;

/** Nombre de jeux auxquels l'utilisateur a attribue la note {@code rating}. */
public record RatingBucketDto(int rating, long count) {
}
