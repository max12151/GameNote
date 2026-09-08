package be.technifutur.il.admin;

import be.technifutur.bll.admin.AdminService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dal.user.UserRole;
import be.technifutur.dl.admin.AdminDashboardDto;
import be.technifutur.dl.admin.AdminUserDto;
import be.technifutur.dl.admin.AdminUserPageDto;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * La console d'administration : comptes, rôles, suspensions et chiffres du site.
 * <p>
 * L'identité de l'administrateur qui appelle traverse chaque opération, et pas seulement pour
 * la journaliser : c'est elle qui interdit de se rétrograder ou de se suspendre soi-même, un
 * geste dont aucun écran ne permettrait de revenir.
 */
@Component
public class AdminFacade {

    private final UserService userService;
    private final AdminService adminService;
    private final AdminMapper adminMapper;

    public AdminFacade(UserService userService, AdminService adminService, AdminMapper adminMapper) {
        this.userService = userService;
        this.adminService = adminService;
        this.adminMapper = adminMapper;
    }

    public AdminUserPageDto listUsers(String username, String search, int page, int size) {
        UserEntity me = userService.getByUsername(username);

        List<AdminUserDto> users = adminService.listUsers(search, page, size).stream()
                .map(view -> adminMapper.toDto(view, me.getId()))
                .toList();

        return new AdminUserPageDto(users, adminService.countUsers(search), Math.max(page, 0), size);
    }

    public void setRole(String username, Long targetUserId, String role) {
        UserEntity me = userService.getByUsername(username);

        adminService.setRole(me.getId(), targetUserId, UserRole.valueOf(role.toUpperCase(Locale.ROOT)));
    }

    public void suspend(String username, Long targetUserId, String reason) {
        UserEntity me = userService.getByUsername(username);

        adminService.suspend(me.getId(), targetUserId, reason);
    }

    public void reactivate(String username, Long targetUserId) {
        UserEntity me = userService.getByUsername(username);

        adminService.reactivate(me.getId(), targetUserId);
    }

    public AdminDashboardDto getDashboard() {
        return adminMapper.toDashboardDto(adminService.getDashboard());
    }
}
