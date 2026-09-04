FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/servico-pedidos-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
