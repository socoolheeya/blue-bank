FROM eclipse-temurin:25-jre
WORKDIR /app
ARG JAR_FILE
RUN groupadd --system --gid 10001 bluebank \
    && useradd --system --uid 10001 --gid bluebank --home-dir /app bluebank
COPY --chown=bluebank:bluebank ${JAR_FILE} /app/app.jar
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
