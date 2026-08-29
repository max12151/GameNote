package be.technifutur.dl.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentRequestDto {

    @NotBlank
    @Size(max = 2000)
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
