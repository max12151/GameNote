package be.technifutur.bll.admin;

/**
 * Les chiffres de la console d'administration.
 *
 * @param averageRating moyenne de toutes les notes du site, nulle tant qu'aucune n'existe
 * @param newUsers30Days inscriptions des trente derniers jours, comptes supprimés exclus
 */
public record AdminDashboard(long users,
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
