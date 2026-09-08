# Identifiants IGDB

GameNote lit son catalogue de jeux chez [IGDB](https://www.igdb.com), dont l'API passe par
un compte développeur Twitch. Sans ces identifiants l'application démarre normalement :
seules la recherche, la page Découvrir et les sorties à venir de l'accueil restent
indisponibles, et le reste du site continue de fonctionner.

## Obtenir un couple client-id / client-secret

1. Activer l'authentification à deux facteurs sur le compte Twitch — la console la refuse
   sans, et c'est là que beaucoup s'arrêtent.
2. Créer une application sur <https://dev.twitch.tv/console/apps>.
   URL de redirection : `http://localhost` ; catégorie : *Application Integration*.
3. Relever le **Client ID**, puis générer un **Client Secret**. Le secret n'est affiché
   qu'une seule fois : le perdre oblige à en générer un nouveau.

## Les fournir à l'application

| | Où | Quand |
|---|---|---|
| `.env` à la racine du dépôt | `IGDB_CLIENT_ID`, `IGDB_CLIENT_SECRET` | pile Docker (`docker compose up`) |
| `api/src/main/resources/application-local.yaml` | bloc `igdb:` ci-dessous | lancement depuis l'IDE, profil `local` |
| Variables d'environnement | idem `.env` | production |

```yaml
# application-local.yaml — ignoré par git
igdb:
  client-id: votre_client_id
  client-secret: votre_client_secret
```

Le secret ne doit jamais être versionné. `application-local.yaml` et `.env` figurent tous
les deux dans `.gitignore` ; `.env.example` sert de modèle et reste vide.

## Ce que fait l'application avec

`IgdbTokenService` échange le couple contre un jeton d'application auprès de
`https://id.twitch.tv/oauth2/token`, et le garde en mémoire jusqu'à une minute avant son
expiration. Le secret ne quitte donc jamais le serveur : le navigateur ne voit que les
routes `/api/igdb/**` de GameNote.

## Vérifier

Une fois l'API démarrée :

```bash
curl "http://localhost:8080/api/igdb/games?search=Street%20Fighter%206&limit=5"
```

La réponse est une liste de jeux normalisés — titre, résumé, jaquette en `t_cover_big`,
genres, plateformes, studios — avec des adresses d'images publiques d'IGDB.

Un tableau vide alors que le jeu existe signale presque toujours un jeton refusé : vérifier
les identifiants. Une erreur de connexion signale plutôt un réseau qui filtre
`api.igdb.com`, cas dans lequel les trois pages concernées affichent leur message
d'indisponibilité.
