package be.technifutur.dal.user;

import java.time.OffsetDateTime;

/**
 * Ce que le filtre d'authentification a besoin de savoir d'un compte, et rien de plus.
 * <p>
 * Surtout pas l'avatar : c'est une data URI base64 pouvant peser deux mégaoctets, et ce
 * filtre s'exécute à chaque requête entrante. Charger l'entité entière ferait passer l'image
 * par le réseau et par la mémoire de la JVM pour lire un rôle.
 */
public interface UserAuthView {

    Long getId();

    String getUsername();

    UserRole getRole();

    OffsetDateTime getSuspendedAt();

    OffsetDateTime getDeletedAt();
}
