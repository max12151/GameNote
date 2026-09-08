package be.technifutur.bll.security;

import be.technifutur.dal.user.UserRole;

/**
 * Ce que le filtre d'authentification a besoin de savoir du porteur d'un jeton.
 * <p>
 * Un enregistrement, et non l'entité : cette lecture a lieu à chaque requête entrante, et
 * l'entité traînerait l'avatar — une data URI base64 de plusieurs mégaoctets — jusque dans le
 * contexte de sécurité, pour y lire un rôle.
 *
 * @param active faux dès que le compte est suspendu ou anonymisé. Le jeton, lui, reste
 *               valable jusqu'à son expiration : c'est cette vérification-là qui referme la
 *               porte tout de suite, sans attendre vingt-quatre heures.
 */
public record AuthenticatedUser(Long id, String username, UserRole role, boolean active) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
