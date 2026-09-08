package be.technifutur.dal.reaction;

/** Nombre de membres ayant trouvé utile l'avis {@code commentId}. */
public interface CommentReactionCountView {

    Long getCommentId();

    long getUsefulCount();
}
