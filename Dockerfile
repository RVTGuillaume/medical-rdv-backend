FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM tomcat:10.1-jre21

RUN rm -rf /usr/local/tomcat/webapps/*

# Copier le dossier déjà extrait par Maven (pas besoin d'unzip)
COPY --from=builder /app/target/medical-rdv /usr/local/tomcat/webapps/ROOT

COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/docker-entrypoint.sh"]