FROM openjdk:8-jdk-alpine
VOLUME /tmp
COPY --from=builder build/libs/ci-sample*-SNAPSHOT.jar ci-sample.jar
ENV JAVA_OPTS=""
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /ci-sample.jar" ]
