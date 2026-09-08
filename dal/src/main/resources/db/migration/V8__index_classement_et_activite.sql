-- Points 4 et 10 : index des lectures ajoutées par le fil d'activité et par les filtres du
-- classement.
--
-- Sans eux, filtrer le classement par genre balaie l'intégralité de `game_rating_genre`, et
-- construire le fil d'activité de vingt joueurs suivis balaie toute la table des notes.

CREATE INDEX idx_game_rating_genre_value    ON game_rating_genre (genre);
CREATE INDEX idx_game_rating_platform_value ON game_rating_platform (platform);
CREATE INDEX idx_game_rating_release_date   ON game_rating (release_date);

-- Fil d'activité : « les dernières notes / les derniers avis de ces joueurs-là ».
CREATE INDEX idx_game_rating_user_created   ON game_rating (user_id, created_at DESC);
CREATE INDEX idx_game_comment_user_created  ON game_comment (user_id, created_at DESC);
