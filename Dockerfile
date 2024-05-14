FROM gradle:7.5-jdk17 as builder
USER root
COPY . .
RUN gradle -x test --no-daemon build

FROM gcr.io/distroless/java17
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-kontroll-assignment-catalog-*.jar /data/app.jar
CMD ["/data/app.jar"]
