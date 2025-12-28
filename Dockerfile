FROM openjdk:17-jdk-slim
WORKDIR /app

# Копируем собранный JAR
COPY build/libs/chess-api.jar app.jar

# Устанавливаем таймзону
ENV TZ=Europe/Moscow

# Открываем порт
EXPOSE 8080

# Запускаем приложение
ENTRYPOINT ["java", "-jar", "app.jar"]