package be.technifutur.il.admin;

import be.technifutur.bll.admin.AdminDashboard;
import be.technifutur.dal.user.UserAdminView;
import be.technifutur.dl.admin.AdminDashboardDto;
import be.technifutur.dl.admin.AdminUserDto;
import org.springframework.stereotype.Component;

@Component
public class AdminMapper {

    /**
     * @param currentAdminId identifiant de l'administrateur qui consulte : sa propre ligne
     *                       arrive sans bouton, car se rétrograder ou se suspendre soi-même
     *                       fermerait la porte sans qu'aucun écran la rouvre
     */
    public AdminUserDto toDto(UserAdminView view, Long currentAdminId) {
        return new AdminUserDto(
                view.getId(),
                view.getUsername(),
                view.getEmail(),
                view.getRole() == null ? "USER" : view.getRole().name(),
                view.getCreatedAt(),
                view.getSuspendedAt() != null,
                view.getSuspendedAt(),
                view.getSuspensionReason(),
                view.getDeletedAt() != null,
                view.getRatedGames(),
                view.getComments(),
                !view.getId().equals(currentAdminId) && view.getDeletedAt() == null
        );
    }

    public AdminDashboardDto toDashboardDto(AdminDashboard dashboard) {
        return new AdminDashboardDto(
                dashboard.users(),
                dashboard.activeUsers(),
                dashboard.suspendedUsers(),
                dashboard.deletedUsers(),
                dashboard.newUsers30Days(),
                dashboard.ratings(),
                dashboard.ratedGames(),
                dashboard.averageRating(),
                dashboard.comments(),
                dashboard.lists(),
                dashboard.pendingReports()
        );
    }
}
