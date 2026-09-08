package be.technifutur.gamenote.config;

import be.technifutur.bll.security.AuthenticatedUser;
import be.technifutur.bll.security.JwtService;
import be.technifutur.bll.user.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtService jwtService, UserService userService) {
        this.jwtService = jwtService;
        this.userService = userService;
    }

    /**
     * Authentifie la requête à partir du jeton, puis <em>confirme le compte en base</em>.
     * <p>
     * Un jeton signé prouve seulement qu'une session a été ouverte, pas que le compte est
     * toujours en règle : il vaut vingt-quatre heures, pendant lesquelles un administrateur a
     * pu être rétrogradé ou un compte suspendu. Porter le rôle dans le jeton aurait évité
     * cette lecture, au prix d'une journée entière de droits périmés — un compte suspendu
     * aurait continué de publier jusqu'au lendemain.
     * <p>
     * La lecture reste légère : une projection de quatre colonnes, sans l'avatar, là où les
     * façades chargent de toute façon l'utilisateur complet ensuite.
     * <p>
     * Le principal reste le pseudo, comme avant : {@code authentication.getName()} est ce que
     * lisent tous les contrôleurs, et le rôle voyage dans les autorisations, où Spring
     * Security sait le lire pour {@code hasRole}.
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isValid(token)) {
                authenticate(request, jwtService.extractUsername(token));
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request, String username) {
        Optional<AuthenticatedUser> account = userService.findAuthenticated(username);

        // Compte disparu, suspendu ou anonymisé : on laisse passer la requête sans identité,
        // et le point d'entrée répondra 401. Rejeter ici avec un message propre demanderait
        // d'écrire la réponse dans le filtre, ce que fait déjà l'AuthenticationEntryPoint.
        if (account.isEmpty() || !account.get().active()) {
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                account.get().username(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.get().role().name())));

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
