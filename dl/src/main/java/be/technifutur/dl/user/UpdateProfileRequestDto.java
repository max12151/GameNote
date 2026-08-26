package be.technifutur.dl.user;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequestDto {

    @Size(max = 1000)
    private String bio;

    // Data URI base64 (image recadrée/compressée côté navigateur avant envoi),
    // pas une simple URL : la limite doit couvrir une image, pas juste un lien.
    @Size(max = 2_000_000)
    private String avatarUrl;

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
