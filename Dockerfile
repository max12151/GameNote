# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------------
# Étape 1 : compilation
#
# Une image Maven complète sert à construire, une image sans compilateur à exécuter :
# l'image finale n'embarque ni le JDK, ni Maven, ni les sources.
# ---------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /build
COPY pom.xml .
COPY api/pom.xml api/
COPY bll/pom.xml bll/
COPY dal/pom.xml dal/
COPY dl/pom.xml dl/
COPY il/pom.xml il/
COPY api/src api/src
COPY bll/src bll/src
COPY dal/src dal/src
COPY dl/src dl/src
COPY il/src il/src

# Le dépôt Maven est monté en cache : les dépendances survivent d'une construction à
# l'autre au lieu d'être retéléchargées à chaque modification du code.
#
# Les tests tournent : ce sont des tests unitaires purs, sans base ni réseau. Une
# construction d'image qui les ignorerait ne vérifierait plus rien avant le déploiement.
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B package

# ---------------------------------------------------------------------------------
# Étape 2 : exécution
# ---------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre

# curl sert uniquement à la sonde de santé plus bas, qui permet à docker compose
# d'attendre que l'API réponde vraiment avant de déclarer le service prêt.
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/*

# Un processus applicatif n'a aucune raison d'être root dans son conteneur.
RUN useradd --system --create-home --uid 10001 gamenote
USER gamenote

WORKDIR /app
COPY --from=build --chown=gamenote:gamenote /build/api/target/api-*.jar app.jar

EXPOSE 8080

# Le classement est une route publique qui interroge la base : la sonde vérifie donc
# toute la chaîne, pas seulement que la JVM a démarré.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=12 \
    CMD curl -fsS "http://localhost:8080/api/community/games?page=0&size=1" || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
