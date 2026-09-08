package be.technifutur.dl.admin;

import java.util.List;

/** Une page de la table des comptes. */
public record AdminUserPageDto(List<AdminUserDto> users, long total, int page, int size) {
}
