# ---- Build (Maven + JDK 21) ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# Cache de dependencias: primero el pom.
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
# Codigo y empaquetado (sin tests: los tests corren en CI/local, no en el build de imagen).
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Runtime (solo JRE 21) ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Usuario no-root.
RUN groupadd -r app && useradd -r -g app app
COPY --from=build /build/target/sumaup360-backend-*.jar app.jar
# El service account de Firebase se monta como volumen en /app/secrets (ver compose).
# /app/uploads es el volumen de imagenes subidas (fotos de producto): debe existir y
# pertenecer a 'app' para que el volumen herede esos permisos y el proceso pueda escribir.
RUN mkdir -p /app/secrets /app/uploads && chown -R app:app /app
USER app
EXPOSE 8080
ENTRYPOINT ["java","-XX:MaxRAMPercentage=75.0","-jar","/app/app.jar"]
