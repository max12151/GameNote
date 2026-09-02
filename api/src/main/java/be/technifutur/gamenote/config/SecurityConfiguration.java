package be.technifutur.gamenote.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import be.technifutur.gamenote.api.common.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Origines autorisées à appeler l'API depuis un navigateur.
     * <p>
     * L'adresse du serveur de développement Angular était écrite ici même : impossible de
     * déployer le front ailleurs sans recompiler le back. Elle vient maintenant de la
     * configuration, avec cette même adresse en repli pour que rien ne change en local.
     */
    private final List<String> allowedOrigins;

    public SecurityConfiguration(JwtAuthenticationFilter jwtAuthenticationFilter,
                                 @Value("${gamenote.cors.allowed-origins}") List<String> allowedOrigins) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.allowedOrigins = allowedOrigins;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/igdb/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Quand un contrôleur lève une exception, Spring redirige en interne
                        // vers /error. Sans cette ligne, ce renvoi est lui-même refusé pour un
                        // visiteur anonyme : l'erreur sort alors en 403 au corps vide, quelle
                        // qu'en soit la cause réelle. Le corps de la réponse reste produit par
                        // GlobalExceptionHandler, rien n'est exposé de plus.
                        .requestMatchers("/error").permitAll()
                        // Vitrine de la page d'accueil : le classement du site est une donnée
                        // agrégée, sans rien de personnel, et doit être visible d'un visiteur
                        // pas encore inscrit. La fiche détaillée, elle, reste protégée : elle
                        // expose la note et le commentaire de l'utilisateur courant.
                        .requestMatchers(HttpMethod.GET, "/api/community/games").permitAll()
                        // Une balise <img> ne peut pas porter le jeton JWT ; cf. AvatarController.
                        .requestMatchers(HttpMethod.GET, "/api/users/*/avatar").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling.authenticationEntryPoint(unauthenticatedEntryPoint()))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Réponse aux requêtes sans identité reconnue : 401, et non 403.
     * <p>
     * Sans point d'entrée déclaré, Spring répond 403 à tout le monde, y compris à un jeton
     * expiré ou mal signé. Les deux codes ne disent pourtant pas la même chose : 401
     * signifie « je ne sais pas qui vous êtes », 403 « je le sais, et cela vous est
     * interdit ». Les confondre rendait impossible, côté navigateur, de distinguer une
     * session finie — qu'il faut fermer et rejouer — d'un refus métier légitime, comme
     * effacer le commentaire d'un autre.
     * <p>
     * Le corps reprend la forme des autres erreurs de l'API, pour que le front n'ait qu'une
     * seule structure à lire.
     */
    @Bean
    public AuthenticationEntryPoint unauthenticatedEntryPoint() {
        ObjectMapper mapper = new ObjectMapper();

        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(),
                    new ErrorResponse("Session expirée ou identifiants manquants"));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCrypt avec coût par défaut (10), largement suffisant pour un projet de démo
        return new BCryptPasswordEncoder();
    }
}
