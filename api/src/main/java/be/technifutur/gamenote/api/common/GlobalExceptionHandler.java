package be.technifutur.gamenote.api.common;

import be.technifutur.bll.exception.DuplicateResourceException;
import be.technifutur.bll.exception.ForbiddenOperationException;
import be.technifutur.bll.exception.InvalidCredentialsException;
import be.technifutur.bll.exception.InvalidOperationException;
import be.technifutur.bll.exception.ResourceNotFoundException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenOperation(ForbiddenOperationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(InvalidOperationException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOperation(InvalidOperationException ex) {
        return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
    }

    /**
     * IGDB injoignable — réseau qui filtre, quota atteint, panne chez eux.
     * <p>
     * Sans ce cas, l'appel remontait en 500 : une panne serveur, alors que le serveur va
     * très bien et que c'est une dépendance <em>optionnelle</em> qui manque. 503 dit la
     * chose exacte — le service est temporairement indisponible, réessayer plus tard — et
     * c'est le code qu'attendent les intermédiaires, qui ne mettent pas un 503 en cache.
     * <p>
     * La fiche communautaire d'un jeu non noté traitait déjà ce cas pour son compte, en
     * dégradant en « introuvable » ; la recherche, la page Découvrir et les sorties à
     * venir, elles, ne le traitaient nulle part. Le front sait afficher un message
     * d'indisponibilité pour ces trois blocs : encore fallait-il ne pas lui renvoyer une
     * erreur qui ressemble à un bogue.
     * <p>
     * On journalise sans la pile : la cause est toujours la même et la trace n'apprend
     * rien, alors qu'une dépendance coupée peut produire une erreur par visiteur.
     */
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ErrorResponse> handleIgdbUnavailable(RestClientException ex) {
        log.warn("Appel à IGDB en échec : {}", ex.getMessage());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("Le catalogue de jeux est momentanément indisponible."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(new ErrorResponse(message));
    }
}
