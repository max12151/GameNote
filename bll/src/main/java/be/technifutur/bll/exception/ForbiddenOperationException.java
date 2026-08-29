package be.technifutur.bll.exception;

/** L'utilisateur est bien authentifié mais n'a pas le droit d'effectuer cette action. */
public class ForbiddenOperationException extends RuntimeException {

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
