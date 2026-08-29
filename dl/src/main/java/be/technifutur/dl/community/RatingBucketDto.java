package be.technifutur.dl.community;

/** Nombre de joueurs ayant attribué la note {@code rating} à un jeu. */
public record RatingBucketDto(int rating, long count) {
}
