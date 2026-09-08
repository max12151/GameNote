package be.technifutur.gamenote.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import be.technifutur.gamenote.api.common.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
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
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.GET, "/api/igdb/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // Documentation OpenAPI. Sans ces trois motifs, /swagger-ui.html
                        // tombait sous `anyRequest().authenticated()` et répondait 401 :
                        // la dépendance springdoc était embarquée sans jamais servir, et
                        // le README promettait une page inaccessible. Le profil `prod`
                        // coupe springdoc à la racine (springdoc.*.enabled: false), ce
                        // qui rend ces autorisations sans objet en production.
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui.html").permitAll()
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
                        // Les valeurs des filtres accompagnent ce classement : les réserver aux
                        // membres laisserait un visiteur devant une barre de filtres vide, à
                        // côté d'un classement qu'il a le droit de lire.
                        .requestMatchers(HttpMethod.GET, "/api/community/filters").permitAll()
                        // Une balise <img> ne peut pas porter le jeton JWT ; cf. AvatarController.
                        .requestMatchers(HttpMethod.GET, "/api/users/*/avatar").permitAll()
                        // La console d'administration. Le rôle vient des autorisations posées
                        // par JwtAuthenticationFilter, qui les relit en base à chaque requête :
                        // un administrateur rétrogradé perd donc l'accès immédiatement, sans
                        // attendre l'expiration de son jeton.
                        //
                        // Le contrôle est déclaré ici plutôt que dans chaque contrôleur : une
                        // route d'administration ajoutée demain sera couverte sans que
                        // personne ait à y penser.
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticatedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()))
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
        /*
         * Instance créée à la main, et non injectée depuis le contexte : Spring Boot 4
         * n'enregistre plus de bean `com.fasterxml.jackson.databind.ObjectMapper`.
         * Demander ce type en paramètre empêche l'application de démarrer — vérifié.
         * La sérialisation dont il s'agit ici est celle d'un record à un seul champ, sur
         * laquelle aucun réglage global n'aurait d'effet visible.
         */
        ObjectMapper mapper = new ObjectMapper();

        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(),
                    new ErrorResponse("Session expirée ou identifiants manquants"));
        };
    }

    /**
     * Réponse à une requête authentifiée mais interdite : 403, avec un corps.
     * <p>
     * Sans ce gestionnaire, un membre ordinaire qui tape une adresse de la console
     * d'administration reçoit un 403 au corps vide, que le front ne peut pas distinguer d'une
     * panne. Le message reprend la forme des autres erreurs de l'API, et ne dit rien de ce
     * qui se trouve derrière : seulement que ce n'est pas pour lui.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        ObjectMapper mapper = new ObjectMapper();

        return (request, response, exception) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(response.getWriter(),
                    new ErrorResponse("Vous n'avez pas les droits nécessaires pour cette action"));
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
}
