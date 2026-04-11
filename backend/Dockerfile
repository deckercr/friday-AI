FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn clean package -q -DskipTests

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=builder /build/target/friday-backend-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
