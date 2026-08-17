package be.technifutur.dl.user;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequestDto {

    @Size(max = 1000)
    private String bio;

    @Size(max = 2048)
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
