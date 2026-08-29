package be.technifutur.gamenote.config;

import be.technifutur.bll.user.UserService;
import be.technifutur.dal.user.UserRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Applique la liste {@code gamenote.admin-usernames} au démarrage.
 * <p>
 * Un compte listé mais pas encore inscrit est simplement ignoré : il sera promu au
 * prochain démarrage, une fois inscrit. L'opération est idempotente, relancer
 * l'application ne fait rien si les rôles sont déjà en place.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserService userService;
    private final AdminProperties adminProperties;

    public AdminBootstrap(UserService userService, AdminProperties adminProperties) {
        this.userService = userService;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String username : adminProperties.getAdminUsernames()) {
            if (username == null || username.isBlank()) {
                continue;
            }

            if (userService.setRole(username.strip(), UserRole.ADMIN)) {
                log.info("Compte '{}' promu administrateur", username.strip());
            }
        }
    }
}
