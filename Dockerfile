# ============================================================
#  STAGE 1 — Build du WAR avec Maven + JDK 21
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

# Copier pom.xml en premier pour cacher les dépendances Maven
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copier les sources et compiler
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================================
#  STAGE 2 — Runtime Tomcat 10.1 + JRE 21
# ============================================================
FROM tomcat:10.1-jre21

# Supprimer les apps par défaut de Tomcat
RUN rm -rf /usr/local/tomcat/webapps/*

# Déployer le WAR en ROOT.war (Tomcat l'extrait automatiquement au démarrage)
COPY --from=builder /app/target/medical-rdv.war /usr/local/tomcat/webapps/ROOT.war

# Copier le script de démarrage
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080

ENTRYPOINT ["/docker-entrypoint.sh"]