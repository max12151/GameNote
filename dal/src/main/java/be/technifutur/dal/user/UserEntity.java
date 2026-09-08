package be.technifutur.dal.user;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    // TEXT (pas de longueur fixe) car l'avatar est stocké en data URI base64
    // (recadré/compressé côté navigateur), pas comme une simple URL courte.
    @Column(name = "avatar_url", columnDefinition = "text")
    private String avatarUrl;

    @Column(columnDefinition = "text")
    private String bio;

    // Colonne volontairement nullable : avec ddl-auto=update, Hibernate ne peut pas ajouter
    // une colonne NOT NULL à une table déjà peuplée. Les comptes créés avant l'arrivée des
    // rôles ont donc role = null, que getRole() interprète comme USER.
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private UserRole role = UserRole.USER;

    /**
     * Date de suspension par un administrateur, nulle tant que le compte est actif. Un compte
     * suspendu ne peut plus se connecter ni rien publier ; ce qu'il a déjà écrit reste en
     * place, la suspension n'est pas une suppression.
     */
    @Column(name = "suspended_at")
    private OffsetDateTime suspendedAt;

    @Column(name = "suspension_reason", length = 255)
    private String suspensionReason;

    /**
     * Date d'anonymisation, nulle pour un compte ordinaire.
     * <p>
     * Supprimer son compte ne supprime pas la ligne : le pseudo devient « Compte supprimé »,
     * l'e-mail et l'empreinte du mot de passe sont rendus inutilisables, l'avatar et la
     * biographie sont effacés. Les notes et les avis restent, faute de quoi le classement du
     * site perdrait ses données et les fils de discussion se retrouveraient troués.
     */
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters / setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public UserRole getRole() {
        return role != null ? role : UserRole.USER;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public boolean isAdmin() {
        return getRole() == UserRole.ADMIN;
    }

    public OffsetDateTime getSuspendedAt() {
        return suspendedAt;
    }

    public void setSuspendedAt(OffsetDateTime suspendedAt) {
        this.suspendedAt = suspendedAt;
    }

    public String getSuspensionReason() {
        return suspensionReason;
    }

    public void setSuspensionReason(String suspensionReason) {
        this.suspensionReason = suspensionReason;
    }

    public OffsetDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(OffsetDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isSuspended() {
        return suspendedAt != null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    /** Un compte suspendu ou anonymisé n'ouvre plus de session et ne publie plus rien. */
    public boolean isActive() {
        return !isSuspended() && !isDeleted();
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}