package be.technifutur.dal.report;

/** Où en est un signalement dans la file de modération. */
public enum ReportStatus {

    /** En attente de décision : c'est ce que la console affiche en premier. */
    PENDING,

    /** Signalement retenu, l'avis a été supprimé. */
    ACCEPTED,

    /** Signalement écarté, l'avis reste en place. */
    REJECTED
}
