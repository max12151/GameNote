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

La documentation OpenAPI n'existe qu'en développement : le profil `prod` coupe springdoc,
et la carte complète de l'API n'est donc pas publiée en production.

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
./mvnw install
cd api && ../mvnw spring-boot:run
```

L'API écoute sur `http://localhost:8080`, la documentation OpenAPI sur
`http://localhost:8080/swagger-ui.html`.

> Si `mvnw` s'arrête avant même de démarrer, vérifier `JAVA_HOME` : un chemin dupliqué du
> type `...\jdk-25.0.2\jdk-25.0.2` produit une erreur peu explicite.

## Tests

```bash
./mvnw test
```

Cinquante-deux tests unitaires, sans base ni réseau : ils tournent partout, y compris dans la
construction de l'image Docker, qui ne les saute plus.

| Ce qui est couvert | Pourquoi celui-là |
|---|---|
| `UserServiceTest` | ce qu'un compte suspendu ou anonymisé n'a plus le droit de faire. Ces deux états ne changent rien à l'écran tant qu'aucun compte n'y est : une régression y passerait inaperçue jusqu'au jour où une suspension ne suspendrait plus rien |
| `IgdbGameDtoTest` | la conversion IGDB → front décide de ce que le navigateur reçoit : protocole ajouté aux adresses, jaquette passée en grand format, studios rangés par rôle, listes absentes rendues vides |
| `CommunityServiceTest` | les deux constantes de la pondération. Une erreur ici ne casse rien : elle réordonne silencieusement la page la plus visible du site |
| `RankingQueryTest` | la normalisation des filtres, qui viennent d'une URL donc de n'importe où : intervalle d'années remis à l'endroit, valeurs aberrantes bornées, tri inconnu ramené au défaut |
| `AvatarServiceTest` | la liste blanche des types d'image. C'est elle qui empêche de faire servir du HTML depuis le domaine de l'API |
| `AccountServiceTest` | l'anonymisation : ce qui doit disparaître, ce qui doit rester, et l'empreinte de mot de passe rendue inutilisable. Une opération sans retour mérite un filet |
| `GameListServiceTest` | la renumérotation des listes. Un déplacement raté ne lève rien : il réordonne le classement de quelqu'un sans qu'il l'ait demandé |
| `ActivityServiceTest` | la fusion des deux sources du fil, et le dédoublonnage d'un jeu noté puis commenté |

Ce qui manque encore : les contrôleurs et les repositories, qui demandent un contexte
Spring et une base.

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

## Schéma et migrations

Le schéma appartient à **Flyway** (`dal/src/main/resources/db/migration`), plus à Hibernate :
`ddl-auto` vaut `validate`, et un écart entre les entités et la base fait échouer le
démarrage au lieu de produire une colonne fantôme découverte trois semaines plus tard.

Les installations existantes ne sont pas cassées pour autant. `baseline-on-migrate` marque
une base déjà peuplée à la **V1** — le schéma tel qu'Hibernate le générait — sans la
rejouer, puis applique les migrations suivantes. Une base vide, elle, reçoit toute la série
depuis la V1. C'est vrai du poste de développement comme de la pile docker, dont le script
d'initialisation recrée le schéma d'origine avant que l'application ne démarre.

Pour ajouter une évolution : un fichier `V<n>__description.sql` dans ce dossier. Ne jamais
modifier une migration déjà appliquée quelque part — Flyway en garde l'empreinte et refuserait
de démarrer.

## Bibliothèque, statuts et notes

Une ligne de `game_rating` n'est plus « une note » mais **une entrée de bibliothèque** :
`à jouer`, `en cours`, `terminé`, `abandonné`, avec ou sans note. Un jeu qu'on veut essayer
y figure donc sans qu'on ait à prétendre y avoir joué.

Conséquence à ne pas manquer : toutes les agrégations du classement excluent explicitement
les lignes sans note. Sans cela, un jeu que dix joueurs veulent essayer compterait dix votes.
Commenter reste conditionné à une **note** — un avis sans note affichée à côté n'aurait rien
à quoi se rattacher.

## Suivi, fil d'activité et réactions

Suivre quelqu'un ne demande pas son accord et ne crée pas le lien inverse : c'est un
abonnement à ce qu'il publie. `GET /api/feed` fond les notes et les avis des joueurs suivis
en un seul fil trié par date. Les deux sources vivent dans deux tables et se lisent par deux
requêtes, chacune ramenant la fenêtre entière avant la fusion : ne demander que la taille
d'une page à chacune produirait un fil qui saute des entrées. Quand un joueur a noté un jeu
*et* laissé son avis dessus, seul l'avis est retenu — il porte déjà la note.

Un avis se marque **utile**, et le fil d'un jeu se trie par utilité. Un seul geste, pas de
pouce vers le bas : un vote négatif enterre les avis minoritaires et fait doublon avec le
signalement, qui existe pour ce qui n'a rien à faire sur le site.

## Supprimer son compte

`PUT /api/users/me/deletion` **anonymise** au lieu d'effacer : le pseudo devient
« Compte supprimé #42 », l'adresse et l'empreinte du mot de passe sont rendues inutilisables,
l'avatar et la biographie sont vidés. Les notes et les avis restent — ce sont des données de
la communauté, les retirer ferait bouger le classement et troueraient des discussions
auxquelles d'autres ont participé.

Disparaissent en revanche ce qui n'a de sens que rattaché à quelqu'un : les liens de suivi
dans les deux sens, les listes personnelles, les réactions données. Le profil public répond
alors 404, et la session en cours est refusée dès la requête suivante.

## Points d'entrée principaux

| Méthode | Route | Accès |
|---|---|---|
| `POST` | `/api/auth/register`, `/api/auth/login` | public |
| `GET` | `/api/community/games` | public — classement pondéré |
| `GET` | `/api/community/games/{id}` | membre — fiche, notes, avis |
| `GET` | `/api/community/comments/recent` | membre — derniers avis du site |
| `GET` | `/api/users/{id}/profile` | membre — profil public, sans e-mail |
| `GET` | `/api/users/{id}/comments` | membre — avis d'un joueur, paginés |
| `GET` | `/api/users/search?q=` | membre — recherche de membres par pseudo |
| `GET` | `/api/users/{id}/avatar` | public — image seule, avec ETag |
| `GET`/`PUT` | `/api/users/me` | membre |
| `POST`/`DELETE` | `/api/comments/...` | membre ; suppression : auteur ou administrateur |
| `PUT` | `/api/users/me/password`, `/api/users/me/email` | membre — mot de passe exigé |
| `PUT` | `/api/users/me/deletion` | membre — suppression par anonymisation |
| `PUT` | `/api/ratings/status` | membre — ranger un jeu sans le noter |
| `GET` | `/api/ratings/library` | membre — compteurs des quatre statuts |
| `POST`/`DELETE` | `/api/users/{id}/follow` | membre — suivre un joueur |
| `GET` | `/api/users/{id}/followers`, `/api/users/{id}/following` | membre |
| `GET` | `/api/feed` | membre — fil d'activité des joueurs suivis |
| `POST`/`DELETE` | `/api/comments/{id}/useful` | membre — marquer un avis utile |
| `POST` | `/api/comments/{id}/reports` | membre — signaler un avis |
| `GET`/`POST`/`PUT`/`DELETE` | `/api/lists/**` | membre — listes personnalisées |
| `GET` | `/api/community/filters` | public — genres et plateformes du classement |
| `GET`/`PUT`/`POST`/`DELETE` | `/api/admin/**` | **administrateur** |
| `GET` | `/api/igdb/**` | public |

### Recherche de membres

`GET /api/users/search?q=&limit=` rend les membres dont le pseudo contient le terme, les
pseudos qui **commencent** par le terme en tête — chercher `max` doit donner Max avant
Maxwell, et Maxwell avant Klimax.

Deux garde-fous. Le terme doit faire au moins deux caractères, sans quoi la réponse est
vide : chercher `a` n'a pas de sens, et laisser passer la requête reviendrait à publier
l'annuaire des comptes du site à qui vide le champ. Et `limit` est plafonné à 30, une
valeur d'URL ne devant pas pouvoir demander la table entière.

La réponse est volontairement pauvre — pseudo, présence d'un avatar, nombre de jeux notés.
Ce dernier vient d'une seule requête d'agrégat pour toute la liste, et non d'un appel par
ligne affichée.

### Avis d'un joueur, page par page

Le profil public sert la première page (dix avis) et le total. Les suivantes se demandent
à `GET /api/users/{id}/comments?page=&size=`, sans recharger les statistiques, la
répartition par genre ni le coup de cœur, qui n'ont pas bougé.

Le front n'a pas besoin de connaître la taille de page : tant qu'il reste des avis à
charger, c'est que la page précédente était pleine — sa longueur *est* donc la taille de
page du serveur. Les deux ne peuvent pas diverger.

### Quand IGDB est injoignable

Réseau qui filtre `api.igdb.com`, quota atteint, panne : les routes qui en dépendent
(`/api/igdb/**`, `/api/discover/games`) répondent **503** avec un message lisible, et non
500. Le serveur va bien ; c'est une dépendance optionnelle qui manque, et 503 est le seul
code qui dise cela — les intermédiaires ne le mettent d'ailleurs pas en cache. Le front
affiche alors un message d'indisponibilité sur les trois blocs concernés, et le reste du
site — classement, collection, profils, avis — continue de fonctionner normalement.

La fiche communautaire d'un jeu que personne n'a noté fait exception : elle dégrade en
404 plutôt qu'en 503, la seule information réellement absente étant le descriptif d'un jeu
inconnu de la base.

### Le classement

L'ordre des jeux suit une moyenne pondérée bayésienne, la méthode d'IMDb :

```
score = (v × R + m × C) / (v + m)
```

`R` la moyenne du jeu, `v` son nombre de votes, `C` la moyenne du site et `m` la médiane
des votes. Les deux constantes se recalculent sur les données réelles, et sont gardées dix
minutes en cache : elles demandent une agrégation sur toute la table des notes, et le
classement est la page publique du site. Sans cette pondération, un jeu noté 10 par une
seule personne dominait le classement.

Le classement se filtre par **genre**, **plateforme** et **intervalle d'années**, et
s'ordonne de cinq façons : score pondéré (défaut), moyenne brute, nombre de votes, sortie
récente, ordre alphabétique. Tout est appliqué par la base — filtrer la page déjà chargée
laisserait introuvable un jeu classé au-delà des vingt premiers. Les valeurs proposées dans
les filtres viennent de `GET /api/community/filters`, donc des jeux réellement notés : une
liste écrite à la main finirait par proposer un genre que personne n'a noté.

Genre et plateforme passent par un `exists` et non par une jointure sur la collection :
joindre les genres multiplierait chaque ligne de note par leur nombre, et un jeu à cinq
genres compterait cinq votes.

## Administrateurs

Les comptes listés dans `gamenote.admin-usernames` sont promus au démarrage. Le compte doit
déjà exister, sinon la promotion attend le démarrage suivant. Depuis la console, un
administrateur peut aussi promouvoir et rétrograder sans repasser par la configuration.

Le rôle est relu en base à chaque requête, jamais tiré du jeton : rétrograder ou suspendre
un compte prend effet immédiatement, sans attendre l'expiration d'un jeton valable un jour.
Un administrateur ne peut ni se rétrograder ni se suspendre lui-même — aucun écran ne
permettrait de revenir en arrière.

La console (`/administration`, `/api/admin/**`) réunit :

- le **tableau de bord** : comptes, notes, avis, listes, signalements en attente ;
- la **file de modération** : chaque signalement conserve une copie du texte visé, si bien
  qu'accepter — donc supprimer l'avis — laisse une trace de la décision ;
- la **table des comptes** : rôle, suspension, réactivation. Le pseudo y mène au profil.

Un **compte suspendu disparaît du site**. Il ne peut plus se connecter — le message lui dit
le motif, seule voie pour l'apprendre — et il sort de tout ce que voient les autres : profil
public en 404, absent de la recherche de membres, des listes d'abonnés, du fil d'activité et
de l'index des listes publiques ; ses avis ne sont plus servis, et les compteurs d'avis
suivent, sinon un classement annoncerait douze avis pour une page qui n'en montre que onze.

Rien n'est effacé pour autant : ses notes continuent de compter dans le classement, où elles
sont anonymes et agrégées, et **la réactivation rend tout d'un coup**. La console, elle,
continue de voir ces comptes — c'est son rôle, et sans cela une suspension serait sans retour.

Le site ne dit jamais qu'un compte est suspendu : il se tait. Afficher « compte suspendu » à
la place d'un avis publierait une décision de modération à tous les visiteurs.

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

- **Couverture partielle** : les tests portent sur la logique pure (conversion IGDB,
  pondération du classement, décodage des avatars, ordre des listes, fusion du fil,
  anonymisation d'un compte). Rien ne vérifie encore les contrôleurs ni les requêtes JPA —
  cela demande un contexte Spring et une base.
- **Pas de révocation de jeton** : se déconnecter oublie le jeton côté navigateur, mais
  celui-ci reste valable jusqu'à son expiration (24 h). Un compte suspendu ou supprimé,
  lui, est refusé dès la requête suivante : le filtre relit son état en base.
- **La visibilité d'un compte se joue dans une dizaine de requêtes JPQL**, chacune portant
  son propre `suspendedAt is null`. JPQL n'offre pas de filtre partagé ; une nouvelle
  lecture publique devra donc penser à reprendre la clause.
- **Pas de limitation des tentatives de connexion.**
- **Pas de confirmation d'adresse e-mail** : le site n'a pas d'infrastructure de mail.
  L'adresse identifie un compte, elle ne prouve pas qu'on le contrôle — et c'est pourquoi
  il n'y a pas non plus de « mot de passe oublié ».
- `GET /api/community/games/{id}` charge tous les commentaires du jeu, sans pagination.
- L'avatar reste stocké en data URI dans `app_user` : chaque lecture de l'utilisateur
  complet le transporte. Les chemins les plus fréquents passent désormais par des
  projections qui l'évitent, mais la colonne n'a pas bougé.
