package be.technifutur.dl.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Changement d'adresse e-mail, sous confirmation du mot de passe. */
public class ChangeEmailRequestDto {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
