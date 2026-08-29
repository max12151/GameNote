package be.technifutur.dal.user;

public enum UserRole {

    /** Compte standard : note et commente ses propres jeux. */
    USER,

    /** Modérateur : peut en plus supprimer le commentaire de n'importe quel joueur. */
    ADMIN
}
