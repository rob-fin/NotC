FROM bitnami/minideb as build

# Install dependencies
RUN apt-get update && apt-get install -y \
    openjdk-11-jdk \
    python3
WORKDIR /build_root/lib
RUN apt-get install -y curl
RUN curl -o ./antlr4.jar https://www.antlr.org/download/antlr-4.9.2-complete.jar
ENV CLASSPATH=.:/build_root/lib/antlr4.jar

# Build sources
WORKDIR /build_root/src
COPY ./src/NotC.g4 .
RUN java org.antlr.v4.Tool -o notc/analysis -package notc.analysis -no-listener -visitor NotC.g4
COPY ./src .
RUN javac -d .. $(find notc -name "*.java")

# Run tests
WORKDIR /build_root
COPY ./tests ./tests
RUN ./tests/run_tests.py

# Create compiler image (at the moment heavily tied to script file notcc)
FROM openjdk:11-jre-slim
WORKDIR compiler_root
COPY --from=build /build_root/notc ./notc
COPY --from=build /build_root/lib/antlr4.jar ./lib/antlr4.jar
ENV CLASSPATH=.:/compiler_root/lib/antlr4.jar

ENTRYPOINT ["java", "notc.Compiler"]
