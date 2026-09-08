package be.technifutur.dl.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Suppression de son propre compte.
 * <p>
 * Le mot de passe est redemandé parce que l'opération est irréversible : le pseudo, l'adresse
 * et l'avatar sont effacés sans retour possible.
 */
public class DeleteAccountRequestDto {

    @NotBlank
    private String currentPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
