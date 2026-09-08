package be.technifutur.dl.report;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Signalement d'un avis par un membre. */
public class ReportCommentRequestDto {

    @NotNull
    private ReportReasonDto reason;

    /** Précision libre, utile surtout quand le motif est « autre ». */
    @Size(max = 500)
    private String details;

    public ReportReasonDto getReason() {
        return reason;
    }

    public void setReason(ReportReasonDto reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
