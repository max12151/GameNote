package be.technifutur.dl.admin;

import java.time.OffsetDateTime;

/**
 * Une ligne de la table des comptes, dans la console d'administration.
 * <p>
 * L'adresse e-mail y figure — c'est le seul écran du site où elle sorte d'un compte autre que
 * le sien, et c'est ce qui permet de distinguer deux pseudos proches lors d'une modération.
 *
 * @param deleted  vrai pour un compte anonymisé : sa ligne demeure, portant les notes et les
 *                 avis conservés, mais il n'y a plus personne derrière
 * @param canActOn faux sur la ligne de l'administrateur qui consulte : se rétrograder ou se
 *                 suspendre soi-même fermerait la porte sans qu'aucun écran la rouvre
 */
public record AdminUserDto(Long id,
                           String username,
                           String email,
                           String role,
                           OffsetDateTime createdAt,
                           boolean suspended,
                           OffsetDateTime suspendedAt,
                           String suspensionReason,
                           boolean deleted,
                           long ratedGames,
                           long comments,
                           boolean canActOn) {
}
