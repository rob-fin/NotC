FROM bitnami/minideb as build
WORKDIR /build_root/src
RUN apt-get update && apt-get install -y \
    bnfc \
    cup \
    jlex \
    make \
    openjdk-11-jdk
ENV CLASSPATH=.:/usr/share/java/JLex.jar:/usr/share/java/cup.jar:/build_root
COPY ./src .

# Generate lexer and parser source code from grammar file
RUN bnfc --java NotC.cf && \
    java JLex.Main NotC/Yylex && \
    java java_cup.Main NotC/NotC.cup

# Build it with the other compiler code
RUN javac -d .. $(find . -name "*.java") 


FROM build as test
WORKDIR /build_root/tests
RUN apt-get update && apt-get install -y python3
COPY ./tests .
RUN python3 run_tests.py


FROM openjdk:11-jre-slim
WORKDIR compiler_root
COPY --from=build /build_root/NotC ./NotC
COPY --from=build /usr/share/java/cup.jar /usr/share/java/cup.jar
ENV CLASSPATH=.:/usr/share/java/cup.jar

ENTRYPOINT ["java", "NotC.Compiler"]
