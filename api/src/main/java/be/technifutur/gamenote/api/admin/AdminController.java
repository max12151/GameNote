package be.technifutur.gamenote.api.admin;

import be.technifutur.dl.admin.AdminDashboardDto;
import be.technifutur.dl.admin.AdminUserPageDto;
import be.technifutur.dl.admin.SuspendUserRequestDto;
import be.technifutur.dl.admin.UpdateRoleRequestDto;
import be.technifutur.dl.report.CommentReportPageDto;
import be.technifutur.dl.report.ReportStatusDto;
import be.technifutur.il.admin.AdminFacade;
import be.technifutur.il.report.ReportFacade;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * La console d'administration : chiffres du site, comptes, et file de modération.
 * <p>
 * Aucune annotation de sécurité ici : {@code /api/admin/**} est réservé au rôle
 * {@code ADMIN} dans {@code SecurityConfiguration}, en un seul endroit. Une route ajoutée
 * plus tard à ce contrôleur est donc protégée sans que personne ait à y penser — c'est
 * l'inverse d'un {@code @PreAuthorize} par méthode, qu'on finit toujours par oublier une fois.
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminFacade adminFacade;
    private final ReportFacade reportFacade;

    public AdminController(AdminFacade adminFacade, ReportFacade reportFacade) {
        this.adminFacade = adminFacade;
        this.reportFacade = reportFacade;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        return ResponseEntity.ok(adminFacade.getDashboard());
    }

    @GetMapping("/users")
    public ResponseEntity<AdminUserPageDto> listUsers(Authentication authentication,
                                                      @RequestParam(required = false) String search,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminFacade.listUsers(authentication.getName(), search, page, size));
    }

    /** Promeut ou rétrograde un compte. Un administrateur ne peut pas s'appliquer l'opération. */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> setRole(Authentication authentication,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody UpdateRoleRequestDto request) {
        adminFacade.setRole(authentication.getName(), userId, request.getRole());

        return ResponseEntity.noContent().build();
    }

    /**
     * Suspend un compte : plus de connexion possible, donc plus rien de publié. Ses avis
     * restent en ligne — la suspension vise la personne, pas ses écrits, que la file de
     * modération traite un par un.
     */
    @PutMapping("/users/{userId}/suspension")
    public ResponseEntity<Void> suspend(Authentication authentication,
                                        @PathVariable Long userId,
                                        @Valid @RequestBody SuspendUserRequestDto request) {
        adminFacade.suspend(authentication.getName(), userId, request.getReason());

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{userId}/suspension")
    public ResponseEntity<Void> reactivate(Authentication authentication, @PathVariable Long userId) {
        adminFacade.reactivate(authentication.getName(), userId);

        return ResponseEntity.noContent().build();
    }

    /**
     * La file de modération.
     *
     * @param status filtre optionnel ; absent, la réponse contient tout l'historique, ce qui
     *               permet de revenir sur les décisions déjà prises
     */
    @GetMapping("/reports")
    public ResponseEntity<CommentReportPageDto> getReports(@RequestParam(required = false) ReportStatusDto status,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(reportFacade.getReports(status, page, size));
    }

    /** Retient le signalement : l'avis est supprimé, et les signalements frères clos avec lui. */
    @PostMapping("/reports/{reportId}/accept")
    public ResponseEntity<Void> acceptReport(Authentication authentication, @PathVariable Long reportId) {
        reportFacade.accept(authentication.getName(), reportId);

        return ResponseEntity.noContent().build();
    }

    /** Écarte le signalement : l'avis reste en place. */
    @PostMapping("/reports/{reportId}/reject")
    public ResponseEntity<Void> rejectReport(Authentication authentication, @PathVariable Long reportId) {
        reportFacade.reject(authentication.getName(), reportId);

        return ResponseEntity.noContent().build();
    }
}
