package be.technifutur.dl.discover;

import jakarta.validation.constraints.NotNull;

public class SkipGameRequestDto {

    @NotNull
    private Long igdbGameId;

    public Long getIgdbGameId() {
        return igdbGameId;
    }

    public void setIgdbGameId(Long igdbGameId) {
        this.igdbGameId = igdbGameId;
    }
}
