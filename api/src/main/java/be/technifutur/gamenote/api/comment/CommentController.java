package be.technifutur.gamenote.api.comment;

import be.technifutur.dl.comment.CommentRequestDto;
import be.technifutur.dl.comment.GameCommentDto;
import be.technifutur.il.comment.CommentFacade;
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

    public CommentController(CommentFacade commentFacade) {
        this.commentFacade = commentFacade;
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
}
