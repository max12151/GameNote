# GameNote — API

Site de notation de jeux vidéo : chacun note ce qu'il a joué, commente, et retrouve le
classement de la communauté. Ce dépôt contient l'API ; l'interface Angular vit dans un
dépôt séparé (`gamenote-ui`).

## Démarrer — tout en conteneurs

Le chemin le plus court sur une machine neuve : ni Java, ni Maven, ni Node à installer.

```bash
git clone https://github.com/max12151/GameNote.git
git clone https://github.com/max12151/gamenote-ui.git
cd GameNote
cp .env.example .env      # y mettre les identifiants IGDB
docker compose up --build
```

**Les deux dépôts doivent être clonés côte à côte** dans le même dossier parent : le
service `web` construit l'interface depuis `../gamenote-ui`.

| | Adresse |
|---|---|
| Site | http://localhost:8090 |
| API | http://localhost:8081 — documentation sur `/swagger-ui.html` |
| Base | `localhost:5440`, base `postgres` |

La base se remplit toute seule au premier démarrage avec `docker/db/init/` : 33 comptes,
830 notes et 330 avis sur 200 jeux, de quoi voir un classement peuplé sans rien saisir.
**Tous les comptes de cet export ouvrent avec `Password123!`**, `Max` étant administrateur.

Cet export est un jeu de démonstration, pas une sauvegarde : les empreintes de mot de passe
des trois comptes personnels y ont été remplacées par celle du mot de passe ci-dessus. Le
dépôt étant public, y publier de vraies empreintes n'aurait rien apporté et aurait exposé
un mot de passe éventuellement réutilisé ailleurs. Une vraie sauvegarde se fait à part :

```bash
docker compose exec db pg_dump -U postgres postgres > sauvegarde.sql
```

Le script d'initialisation n'est rejoué que sur un volume vide. Pour repartir de zéro :

```bash
docker compose down -v && docker compose up --build
```

> **Sur la machine où le projet a été développé**, un conteneur `postgres` occupe déjà le
> port 5440 et empêchera la pile de démarrer. Ses données sont dans l'export ci-dessus :
> `docker rm -f postgres` avant le premier `docker compose up`.

Le port `8080` reste libre : l'API lancée depuis l'IDE et celle du conteneur peuvent
tourner en même temps. La base étant publiée sur `5440`, l'instance de l'IDE s'y connecte
sans changer un réglage — pratique pour développer l'API contre les données de démo.

Pour travailler sur l'interface avec rechargement à chaud, garder la pile en marche et
lancer `npm start` par-dessus dans `gamenote-ui` : le serveur Angular du port 4200 tape
alors sur l'API du port 8080 (celle de l'IDE) ou 8081 (celle du conteneur), selon
`src/environments/environments.ts`.

## Prérequis — installation classique

Seulement si tu ne passes pas par Docker.

| | Version | Remarque |
|---|---|---|
| Java | 25 | `JAVA_HOME` doit pointer sur le JDK, pas sur un sous-dossier |
| Maven | fourni | utiliser `./mvnw`, rien à installer |
| PostgreSQL | 16+ | attendu sur le **port 5440**, base `postgres` |
| Compte Twitch | — | pour les identifiants IGDB, voir plus bas |

Postgres en conteneur :

```bash
docker run --name postgres -e POSTGRES_PASSWORD=postgres -p 5440:5432 -d postgres
```

## Configuration

Tout se configure par variables d'environnement, avec des valeurs de repli pour le poste de
développement (`api/src/main/resources/application.yaml`).

| Variable | Défaut en dev | Rôle |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5440/postgres` | connexion à la base |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `postgres` / `postgres` | identifiants |
| `JWT_SECRET` | secret de dev en clair | signature des jetons, **32 octets minimum** |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | origines du front, séparées par des virgules |
| `IGDB_CLIENT_ID` / `IGDB_CLIENT_SECRET` | *(aucun)* | application Twitch, voir `api/src/main/resources/IGDB_SETUP.md` |

Les identifiants IGDB n'ont pas de repli : sans eux, l'application démarre mais la
recherche, la page Découvrir et les sorties à venir échouent. Le plus simple en local est
de créer `api/src/main/resources/application-local.yaml` — ce fichier est ignoré par git :

```yaml
igdb:
  client-id: votre_client_id
  client-secret: votre_client_secret
```

...puis de lancer avec le profil `local`.

## Lancer

```bash
./mvnw -Dmaven.test.skip=true install
cd api && ../mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8080`, la documentation OpenAPI sur
`http://localhost:8080/swagger-ui.html`.

### Deux pièges connus

- **`-Dmaven.test.skip=true` est nécessaire**, pas seulement `-DskipTests` : la dépendance
  `spring-boot-starter-test` n'est déclarée nulle part, donc `api/src/test` ne compile pas.
  `-DskipTests` compile quand même les tests et échoue. À corriger le jour où le projet
  sera réellement testé.
- Si `mvnw` s'arrête avant même de démarrer, vérifier `JAVA_HOME` : un chemin dupliqué du
  type `...\jdk-25.0.2\jdk-25.0.2` produit une erreur peu explicite.

## Architecture

Cinq modules Maven, avec des dépendances strictement descendantes :

```
api  →  il  →  bll  →  dal        dl (DTO, transverse)
```

| Module | Contenu |
|---|---|
| `api` | contrôleurs REST, sécurité, client IGDB, configuration |
| `il` | façades et mappage entité ↔ DTO |
| `bll` | règles métier, exceptions fonctionnelles |
| `dal` | entités JPA, repositories, projections |
| `dl` | objets de transport, partagés par toutes les couches |

Une couche ne connaît jamais celle qui l'appelle. Les DTO exposés au réseau vivent dans
`dl` et sont distincts des entités : `PublicProfileDto`, par exemple, n'a pas de champ
e-mail — ce n'est donc pas une omission d'affichage mais une impossibilité.

## Points d'entrée principaux

| Méthode | Route | Accès |
|---|---|---|
| `POST` | `/api/auth/register`, `/api/auth/login` | public |
| `GET` | `/api/community/games` | public — classement pondéré |
| `GET` | `/api/community/games/{id}` | membre — fiche, notes, avis |
| `GET` | `/api/community/comments/recent` | membre — derniers avis du site |
| `GET` | `/api/users/{id}/profile` | membre — profil public, sans e-mail |
| `GET` | `/api/users/{id}/avatar` | public — image seule, avec ETag |
| `GET`/`PUT` | `/api/users/me` | membre |
| `POST`/`DELETE` | `/api/comments/...` | membre ; suppression : auteur ou administrateur |
| `GET` | `/api/igdb/**` | public |

### Le classement

L'ordre des jeux suit une moyenne pondérée bayésienne, la méthode d'IMDb :

```
score = (v × R + m × C) / (v + m)
```

`R` la moyenne du jeu, `v` son nombre de votes, `C` la moyenne du site et `m` la médiane
des votes. Les deux constantes se recalculent sur les données réelles à chaque appel. Sans
cette pondération, un jeu noté 10 par une seule personne dominait le classement.

## Administrateurs

Les comptes listés dans `gamenote.admin-usernames` sont promus au démarrage. Le compte doit
déjà exister, sinon la promotion attend le démarrage suivant. Un administrateur peut
supprimer n'importe quel commentaire ; le droit est relu en base à chaque requête, jamais
tiré du jeton — révoquer un rôle prend effet immédiatement.

## Production

```bash
SPRING_PROFILES_ACTIVE=prod \
DATABASE_URL=... DATABASE_USER=... DATABASE_PASSWORD=... \
JWT_SECRET=... CORS_ALLOWED_ORIGINS=https://votre-front \
IGDB_CLIENT_ID=... IGDB_CLIENT_SECRET=... \
java -jar api/target/api-0.0.1-SNAPSHOT.jar
```

Le profil `prod` n'accorde de valeur de repli à rien : une variable oubliée empêche le
démarrage plutôt que de laisser tourner l'application avec le secret de développement. Il
désactive aussi le journal SQL et les traces d'exception renvoyées au client.

## Limites connues

- **Aucun test qui tourne** : `spring-boot-starter-test` n'est pas déclaré.
- **Pas de gestion de schéma** : `ddl-auto: update` laisse Hibernate faire — il n'enlève
  rien, ne renomme pas, ne revient pas en arrière. C'est pour cela que la colonne `role`
  est nullable. Flyway est le prochain chantier ; `ddl-auto` passera alors à `validate`.
- **Pas de révocation de jeton** : se déconnecter oublie le jeton côté navigateur, mais
  celui-ci reste valable jusqu'à son expiration (24 h).
- **Pas de limitation des tentatives de connexion.**
- `GET /api/community/games/{id}` charge tous les commentaires du jeu, sans pagination.
