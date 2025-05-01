FROM eclipse-temurin:21 AS jre-build
RUN $JAVA_HOME/bin/jlink \
        --add-modules java.se,jdk.httpserver,jdk.jcmd,jdk.unsupported \
        --strip-debug \
        --no-man-pages \
        --no-header-files \
        --compress=zip-9 \
        --output /javaruntime

FROM sbtscala/scala-sbt:eclipse-temurin-21.0.6_7_1.10.10_3.6.4 AS jar-build
RUN curl -fsSL https://deb.nodesource.com/setup_22.x -o nodesource_setup.sh && \
    bash nodesource_setup.sh && \
    apt-get install -y nodejs
WORKDIR /arktrio/
COPY arktwin/ /arktrio/
RUN sbt viewer/package edge/assembly

FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
RUN apt-get update && \
    apt-get install -y curl jq && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*
RUN mkdir /opt/arktrio/ && \
    mkdir /etc/opt/arktrio/ && \
    touch /etc/opt/arktrio/edge.conf
COPY --from=jre-build /javaruntime $JAVA_HOME
COPY --from=jar-build /arktrio/edge/target/scala-3.6.4/arktrio-edge.jar /opt/terrafying/arktrio-edge.jar
COPY docker/edge.sh /opt/arktrio/entrypoint.sh
ENTRYPOINT ["/opt/arktrio/entrypoint.sh"]
