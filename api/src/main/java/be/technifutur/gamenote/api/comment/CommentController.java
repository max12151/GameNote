package be.technifutur.gamenote.api.comment;

import be.technifutur.dl.comment.CommentRequestDto;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.dl.report.ReportCommentRequestDto;
import be.technifutur.il.comment.CommentFacade;
import be.technifutur.il.report.ReportFacade;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentFacade commentFacade;
    private final ReportFacade reportFacade;

    public CommentController(CommentFacade commentFacade, ReportFacade reportFacade) {
        this.commentFacade = commentFacade;
        this.reportFacade = reportFacade;
    }

    /**
     * Enregistre l'avis de l'utilisateur sur un jeu. Un second envoi sur le même jeu
     * remplace le commentaire existant : chacun n'en a qu'un seul par jeu.
     */
    @PostMapping("/games/{igdbGameId}")
    public ResponseEntity<GameCommentDto> saveComment(Authentication authentication,
                                                      @PathVariable Long igdbGameId,
                                                      @Valid @RequestBody CommentRequestDto request) {
        return ResponseEntity.ok(commentFacade.saveComment(authentication.getName(), igdbGameId, request));
    }

    /**
     * Supprime un commentaire : le sien, ou celui de n'importe qui pour un administrateur.
     * Les deux cas passent par la même route, l'autorisation étant tranchée côté serveur.
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(Authentication authentication, @PathVariable Long commentId) {
        commentFacade.deleteComment(authentication.getName(), commentId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Marque un avis comme utile.
     * <p>
     * Renvoie le compteur à jour plutôt qu'un 204 : le bouton doit afficher le nouveau
     * total, et le déduire côté front donnerait un chiffre faux dès que quelqu'un d'autre a
     * cliqué entre-temps. Idempotent — un double clic ne compte pas deux fois.
     */
    @PostMapping("/{commentId}/useful")
    public ResponseEntity<Map<String, Long>> markUseful(Authentication authentication,
                                                        @PathVariable Long commentId) {
        return ResponseEntity.ok(Map.of("usefulCount",
                commentFacade.markUseful(authentication.getName(), commentId)));
    }

    @DeleteMapping("/{commentId}/useful")
    public ResponseEntity<Map<String, Long>> unmarkUseful(Authentication authentication,
                                                          @PathVariable Long commentId) {
        return ResponseEntity.ok(Map.of("usefulCount",
                commentFacade.unmarkUseful(authentication.getName(), commentId)));
    }

    /**
     * Signale un avis à la modération.
     * <p>
     * Rien ne se passe visiblement : l'avis reste en ligne jusqu'à ce qu'un administrateur
     * tranche. Le signalement conserve une copie du texte visé, pour que la décision porte
     * bien sur ce qui a été signalé même si l'auteur le modifie entre-temps.
     */
    @PostMapping("/{commentId}/reports")
    public ResponseEntity<Void> report(Authentication authentication,
                                       @PathVariable Long commentId,
                                       @Valid @RequestBody ReportCommentRequestDto request) {
        reportFacade.report(authentication.getName(), commentId, request);

        return ResponseEntity.accepted().build();
    }
}
