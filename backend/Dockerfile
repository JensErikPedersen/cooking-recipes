# Maven 3.9.11 matches the wrapper. The official image is used instead of ./mvnw because the
# wrapper jar is gitignored, so a clean checkout would have to download it during the build.
FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml lombok.config ./
COPY src/ src/
# Tests run on the host (./mvnw clean verify), not in every image build.
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

FROM eclipse-temurin:25-jre
WORKDIR /app
# curl is only for the Compose healthcheck; the JRE image ships neither curl nor wget.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*
RUN useradd --system --no-create-home recipes
# The API writes dates in the JVM's zone without saying which, and the frontend labels them UTC.
# The base image is UTC today; this keeps it so if that ever changes.
ENV TZ=UTC
COPY --from=build --chown=recipes /build/target/recipes-*.jar app.jar
USER recipes
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
