# ----- build -----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
COPY payment-service/pom.xml payment-service/

COPY invoice-service/pom.xml invoice-service/
COPY validation-invoice/pom.xml validation-invoice/
COPY approval-service/pom.xml approval-service/

RUN mvn -pl payment-service -am dependency:go-offline -B || true

COPY payment-service payment-service
COPY invoice-service invoice-service
COPY validation-invoice validation-invoice
COPY approval-service approval-service

RUN mvn -pl payment-service -am package -DskipTests -B

# ----- run -----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/payment-service/target/*.jar app.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]