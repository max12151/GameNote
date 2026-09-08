package be.technifutur.dl.admin;

/**
 * Les chiffres du site, en tête de la console d'administration.
 *
 * @param averageRating moyenne de toutes les notes, nulle tant qu'aucune n'existe
 */
public record AdminDashboardDto(long users,
                                long activeUsers,
                                long suspendedUsers,
                                long deletedUsers,
                                long newUsers30Days,
                                long ratings,
                                long ratedGames,
                                Double averageRating,
                                long comments,
                                long lists,
                                long pendingReports) {
}
