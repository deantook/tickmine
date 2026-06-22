FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY agent-domain/pom.xml agent-domain/
COPY agent-infra/pom.xml agent-infra/
COPY agent-llm/pom.xml agent-llm/
COPY agent-mcp/pom.xml agent-mcp/
COPY agent-planner/pom.xml agent-planner/
COPY agent-executor/pom.xml agent-executor/
COPY agent-api/pom.xml agent-api/
COPY agent-boot/pom.xml agent-boot/

RUN mvn -q dependency:go-offline -pl agent-boot -am

COPY agent-domain agent-domain
COPY agent-infra agent-infra
COPY agent-llm agent-llm
COPY agent-mcp agent-mcp
COPY agent-planner agent-planner
COPY agent-executor agent-executor
COPY agent-api agent-api
COPY agent-boot agent-boot

RUN mvn -q package -pl agent-boot -am -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/agent-boot/target/agent-boot-*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
