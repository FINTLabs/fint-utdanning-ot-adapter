FROM gradle:jdk21 as builder
USER root
COPY . .
RUN gradle --no-daemon build

FROM eclipse-temurin:21-jre
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-utdanning-ot-adapter*.jar /data/app.jar
ENTRYPOINT ["java", "-jar", "/data/app.jar"]