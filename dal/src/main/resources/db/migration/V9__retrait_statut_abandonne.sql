-- Retrait du statut « abandonné ».
--
-- Quatre statuts pour une bibliothèque, c'était un de trop : « abandonné » et « terminé »
-- disent tous deux qu'on a arrêté de jouer, et la nuance n'apportait rien qu'un onglet de
-- plus à lire. Trois suffisent : à jouer, en cours, terminé.
--
-- Les entrées qui portaient ce statut basculent en « terminé » : le joueur y a joué, et
-- c'est la seule chose que le statut restant sache encore dire. La contrainte est refaite
-- ensuite, faute de quoi elle refuserait la nouvelle liste de valeurs.

UPDATE game_rating SET status = 'FINISHED' WHERE status = 'ABANDONED';

ALTER TABLE game_rating DROP CONSTRAINT game_rating_status_check;

ALTER TABLE game_rating
    ADD CONSTRAINT game_rating_status_check
        CHECK (status IN ('WISHLIST', 'PLAYING', 'FINISHED'));
