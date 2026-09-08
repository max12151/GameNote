package be.technifutur.bll.community;

import java.util.Locale;

/** Les ordres proposés sur le classement du site. */
public enum RankingSort {

    /**
     * Score pondéré bayésien : l'ordre par défaut, celui qui empêche un 10 unique de battre un
     * 9,2 confirmé par onze joueurs.
     */
    WEIGHTED,

    /** Moyenne brute, sans pondération — utile pour voir ce que la pondération corrige. */
    AVERAGE,

    /** Les jeux dont on parle le plus, quel que soit ce qu'on en dit. */
    VOTES,

    /** Sorties les plus récentes d'abord. */
    RECENT,

    /** Ordre alphabétique. */
    TITLE;

    /**
     * Convertit ce qui arrive de l'URL, sans jamais échouer : une valeur inconnue retombe sur
     * le classement par défaut du site plutôt que de renvoyer une erreur.
     */
    public static RankingSort parse(String value) {
        if (value == null) {
            return WEIGHTED;
        }

        try {
            return RankingSort.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            return WEIGHTED;
        }
    }
}
