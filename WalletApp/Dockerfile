
FROM openjdk:17-jdk-slim
VOLUME /tmp
WORKDIR /app
COPY target/WalletApp-0.0.1-SNAPSHOT.jar /app/WalletApp-0.0.1-SNAPSHOT.jar
ARG JAR_FILE=target/*.jar
EXPOSE 8080/tcp
ENTRYPOINT ["java", "-jar", "WalletApp-0.0.1-SNAPSHOT.jar"]


