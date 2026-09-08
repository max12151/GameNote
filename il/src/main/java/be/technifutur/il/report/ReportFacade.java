package be.technifutur.il.report;

import be.technifutur.bll.report.CommentReportService;
import be.technifutur.bll.user.UserService;
import be.technifutur.dal.report.ReportStatus;
import be.technifutur.dal.user.UserEntity;
import be.technifutur.dl.report.CommentReportDto;
import be.technifutur.dl.report.CommentReportPageDto;
import be.technifutur.dl.report.ReportCommentRequestDto;
import be.technifutur.dl.report.ReportStatusDto;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Le signalement d'un avis, côté membre, et la file de modération, côté administrateur.
 */
@Component
public class ReportFacade {

    private final UserService userService;
    private final CommentReportService commentReportService;
    private final CommentReportMapper commentReportMapper;

    public ReportFacade(UserService userService,
                        CommentReportService commentReportService,
                        CommentReportMapper commentReportMapper) {
        this.userService = userService;
        this.commentReportService = commentReportService;
        this.commentReportMapper = commentReportMapper;
    }

    public void report(String username, Long commentId, ReportCommentRequestDto request) {
        UserEntity me = userService.getByUsername(username);

        commentReportService.report(
                commentId,
                me.getId(),
                be.technifutur.dal.report.ReportReason.valueOf(request.getReason().name()),
                request.getDetails());
    }

    /**
     * La file de modération.
     *
     * @param status filtre optionnel ; absent, l'écran montre tout l'historique, décisions
     *               passées comprises — c'est ce qui permet de revenir sur ce qui a été fait
     */
    public CommentReportPageDto getReports(ReportStatusDto status, int page, int size) {
        ReportStatus filter = status == null ? null : ReportStatus.valueOf(status.name());

        List<CommentReportDto> reports = commentReportService.getReports(filter, page, size).stream()
                .map(commentReportMapper::toDto)
                .toList();

        return new CommentReportPageDto(
                reports,
                commentReportService.countReports(filter),
                commentReportService.countPending(),
                Math.max(page, 0),
                size);
    }

    public void accept(String username, Long reportId) {
        commentReportService.accept(reportId, userService.getByUsername(username).getId());
    }

    public void reject(String username, Long reportId) {
        commentReportService.reject(reportId, userService.getByUsername(username).getId());
    }
}
