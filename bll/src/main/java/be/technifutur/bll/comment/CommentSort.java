package be.technifutur.bll.comment;

/** Ordre du fil d'avis d'un jeu. */
public enum CommentSort {

    /** Le plus récent d'abord : l'ordre de lecture par défaut d'une discussion. */
    RECENT,

    /** Les avis que la communauté a le plus marqués comme utiles, puis les plus récents. */
    USEFUL;

    /**
     * Convertit ce qui arrive de l'URL, sans jamais échouer : une valeur inconnue retombe sur
     * l'ordre chronologique plutôt que de renvoyer une erreur pour un paramètre décoratif.
     */
    public static CommentSort parse(String value) {
        if (value == null) {
            return RECENT;
        }

        try {
            return CommentSort.valueOf(value.strip().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return RECENT;
        }
    }
}
