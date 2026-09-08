-- Point 3 : statut de jeu (« à jouer », « en cours », « terminé », « abandonné »).
--
-- La ligne de `game_rating` cesse d'être « une note » pour devenir « une entrée de
-- bibliothèque » : un jeu qu'on veut jouer y figure sans note, une fois joué la note
-- s'ajoute sur la même ligne. La table garde son nom, que quatre tables de collection et
-- une douzaine de requêtes référencent ; la renommer n'aurait rien apporté qu'un risque.
--
-- Conséquence à ne pas manquer : toutes les agrégations du classement doivent désormais
-- exclure explicitement les lignes sans note, sinon un jeu simplement mis en attente par
-- dix joueurs compterait comme dix votes.

ALTER TABLE game_rating ALTER COLUMN rating DROP NOT NULL;

ALTER TABLE game_rating ADD COLUMN status varchar(20);

-- Les lignes existantes portent toutes une note : ce sont des jeux joués.
UPDATE game_rating SET status = 'FINISHED' WHERE status IS NULL;

ALTER TABLE game_rating ALTER COLUMN status SET NOT NULL;

ALTER TABLE game_rating
    ADD CONSTRAINT game_rating_status_check
        CHECK (status IN ('WISHLIST', 'PLAYING', 'FINISHED', 'ABANDONED'));

-- « Ma liste à jouer » et « ce que je joue en ce moment » sont les deux lectures
-- quotidiennes de la bibliothèque.
CREATE INDEX idx_game_rating_user_status ON game_rating (user_id, status);
