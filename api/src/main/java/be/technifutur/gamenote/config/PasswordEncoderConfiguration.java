package be.technifutur.gamenote.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * L'encodeur de mots de passe, dans sa propre configuration.
 * <p>
 * Il vivait avec la chaine de filtres, ce qui a fini par former un cycle : le filtre
 * d'authentification lit desormais le compte en base, donc depend de UserService, qui depend
 * de l'encodeur, defini par la configuration qui recevait le filtre. Spring refuse de
 * demarrer sur un tel cycle, et il avait raison de le signaler : hacher un mot de passe et
 * cabler une chaine de filtres HTTP ne sont pas la meme responsabilite.
 */
@Configuration
public class PasswordEncoderConfiguration {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt avec cout par defaut (10), largement suffisant pour un projet de demo
        return new BCryptPasswordEncoder();
    }
}
