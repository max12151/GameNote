package be.technifutur.dal.list;

import java.time.OffsetDateTime;

/**
 * Une liste telle qu'elle apparaît dans un index : son identité, son nombre de jeux et de
 * quoi nommer son auteur.
 * <p>
 * Le compte vient d'une sous-requête portée par la requête principale, et non d'un appel par
 * ligne affichée.
 */
public interface GameListSummaryView {

    Long getId();

    String getName();

    String getDescription();

    ListVisibility getVisibility();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();

    Long getOwnerId();

    String getOwnerUsername();

    long getItemCount();
}
