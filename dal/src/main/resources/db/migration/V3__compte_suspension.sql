-- Points 1 et 13 : suspension d'un compte par un administrateur, et suppression de compte
-- par anonymisation.
--
-- `deleted_at` marque un compte anonymisé : pseudo remplacé, e-mail et empreinte de mot de
-- passe rendus inutilisables, avatar et biographie effacés. Les notes et les avis restent,
-- signés d'un « Compte supprimé » — le classement du site ne perd pas ses données et aucun
-- fil de discussion ne se retrouve troué.

ALTER TABLE app_user ADD COLUMN suspended_at      timestamp(6) with time zone;
ALTER TABLE app_user ADD COLUMN suspension_reason varchar(255);
ALTER TABLE app_user ADD COLUMN deleted_at        timestamp(6) with time zone;
