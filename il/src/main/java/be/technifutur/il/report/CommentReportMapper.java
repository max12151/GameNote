package be.technifutur.il.report;

import be.technifutur.dal.report.CommentReportView;
import be.technifutur.dl.report.CommentReportDto;
import be.technifutur.dl.report.ReportReasonDto;
import be.technifutur.dl.report.ReportStatusDto;
import org.springframework.stereotype.Component;

@Component
public class CommentReportMapper {

    public CommentReportDto toDto(CommentReportView view) {
        return new CommentReportDto(
                view.getId(),
                view.getCommentId(),
                view.getIgdbGameId(),
                view.getReportedContent(),
                ReportReasonDto.valueOf(view.getReason().name()),
                view.getDetails(),
                ReportStatusDto.valueOf(view.getStatus().name()),
                view.getCreatedAt(),
                view.getHandledAt(),
                view.getReporterId(),
                view.getReporterUsername(),
                view.getReportedAuthorId(),
                view.getReportedAuthorUsername(),
                view.getHandledByUsername(),
                view.getCommentStillExists()
        );
    }
}
