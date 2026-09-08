package be.technifutur.dl.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Promotion ou rétrogradation d'un compte. */
public class UpdateRoleRequestDto {

    @NotBlank
    @Pattern(regexp = "USER|ADMIN", message = "doit valoir USER ou ADMIN")
    private String role;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
