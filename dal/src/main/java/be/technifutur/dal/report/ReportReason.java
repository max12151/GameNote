package be.technifutur.dal.report;

/** Motif choisi par le membre qui signale un avis. */
public enum ReportReason {

    /** Publicité, contenu répété, hors-sujet commercial. */
    SPAM,

    /** Insultes, harcèlement, propos haineux. */
    ABUSE,

    /** Révèle l'intrigue sans avertissement. */
    SPOILER,

    /** Ne parle pas du jeu. */
    OFF_TOPIC,

    /** Autre motif, détaillé en texte libre. */
    OTHER
}
