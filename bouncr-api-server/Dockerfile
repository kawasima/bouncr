# Stage 1: Build with Maven
FROM eclipse-temurin:25-jdk AS build
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

WORKDIR /src

# Copy parent POM and all module POMs first.
# All modules listed in the parent POM must have their pom.xml present.
COPY pom.xml ./
COPY bouncr-components/pom.xml bouncr-components/
COPY bouncr-api-server/pom.xml bouncr-api-server/
COPY bouncr-hook-email/pom.xml bouncr-hook-email/
COPY bouncr-hook-license/pom.xml bouncr-hook-license/
COPY bouncr-api-e2e-test/pom.xml bouncr-api-e2e-test/

# Download dependencies in a separate layer (cached unless POMs change)
RUN mvn dependency:go-offline -pl bouncr-api-server -am -B -q || true

# Copy source code (this layer invalidates on source changes, but deps are cached above)
COPY bouncr-components/src bouncr-components/src
COPY bouncr-api-server/src bouncr-api-server/src

# Install parent + components + api-server into local repo in one reactor pass,
# then copy runtime dependencies into a lib directory.
RUN mvn install -pl bouncr-api-server -am -B -DskipTests \
    -Dmaven.javadoc.skip=true && \
    mvn dependency:copy-dependencies -pl bouncr-api-server \
    -DoutputDirectory=target/lib -DincludeScope=runtime -B -q

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app
COPY --from=build /src/bouncr-api-server/target/bouncr-api-server-*.jar ./app.jar
COPY --from=build /src/bouncr-api-server/target/lib/ ./lib/
# bouncr-components is excluded from copy-dependencies by groupId filter
COPY --from=build /src/bouncr-components/target/bouncr-components-*.jar ./lib/

ENTRYPOINT ["java", "-cp", "app.jar:lib/*", "net.unit8.bouncr.api.Main"]
