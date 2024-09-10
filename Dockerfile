FROM gradle:8.10-jdk21 as builder
USER root
COPY . .
RUN gradle -x test --no-daemon build

FROM gcr.io/distroless/java21
ENV JAVA_TOOL_OPTIONS -XX:+ExitOnOutOfMemoryError
COPY --from=builder /home/gradle/build/libs/fint-kontroll-assignment-catalog-*.jar /data/app.jar
CMD ["/data/app.jar"]
