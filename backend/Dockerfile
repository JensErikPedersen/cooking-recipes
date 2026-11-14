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
# The pictures' files (docs/PLAN_V8.md decision 1), on the pictures-data volume. A named volume takes
# the owner of the image's directory when it is first created; without this it would be root's, and
# the backend could not write a picture.
RUN mkdir -p /data/pictures && chown recipes /data/pictures
ENV PICTURES_DIR=/data/pictures
# Not load-bearing: dates are stored in UTC whatever the JVM's zone, and the API sends them with
# their offset. It keeps the JVM's own clock - logs, the dates in a write's response - in UTC too.
ENV TZ=UTC
COPY --from=build --chown=recipes /build/target/recipes-*.jar app.jar
USER recipes
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
