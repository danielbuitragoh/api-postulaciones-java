# Etapa 1: compilar el jar con Maven (sin pruebas; las pruebas corren en CI y en local).
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q dependency:go-offline
COPY src src
RUN ./mvnw -q package -DskipTests

# Etapa 2: imagen final solo con el JRE y el jar, sin usuario root.
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --no-create-home api
COPY --from=build /app/target/*.jar app.jar
USER api
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
