package be.technifutur.bll.exception;

/** L'action demandée est refusée par une règle métier (et non par un droit manquant). */
public class InvalidOperationException extends RuntimeException {

    public InvalidOperationException(String message) {
        super(message);
    }
}
