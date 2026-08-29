package be.technifutur.gamenote.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gamenote")
public class AdminProperties {

    /**
     * Comptes à promouvoir administrateur au démarrage. Sert à créer le tout premier
     * modérateur, qu'aucun autre administrateur ne peut promouvoir puisqu'il n'en existe
     * encore aucun.
     */
    private List<String> adminUsernames = new ArrayList<>();

    public List<String> getAdminUsernames() {
        return adminUsernames;
    }

    public void setAdminUsernames(List<String> adminUsernames) {
        this.adminUsernames = adminUsernames;
    }
}
