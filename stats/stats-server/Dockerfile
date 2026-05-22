FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY . .

RUN mvn -q -pl stats/stats-server -am clean package -DskipTests

FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY --from=build /build/stats/stats-server/target/*.jar /app.jar

EXPOSE 9090
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar /app.jar"]