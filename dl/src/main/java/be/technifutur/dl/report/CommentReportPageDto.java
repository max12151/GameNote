package be.technifutur.dl.report;

import java.util.List;

/** Une page de la file de modération, avec son total et le nombre de signalements en attente. */
public record CommentReportPageDto(List<CommentReportDto> reports,
                                   long total,
                                   long pending,
                                   int page,
                                   int size) {
}
