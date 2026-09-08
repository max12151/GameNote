package be.technifutur.dl.admin;

import jakarta.validation.constraints.Size;

/**
 * Suspension d'un compte.
 *
 * @see #getReason() motif, facultatif mais affiché au titulaire lorsqu'il tente de se
 *      connecter : c'est la seule voie par laquelle il peut l'apprendre
 */
public class SuspendUserRequestDto {

    @Size(max = 255)
    private String reason;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
