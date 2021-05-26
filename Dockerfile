FROM bitnami/minideb as build
WORKDIR /build_root/src

# Dependencies
RUN apt-get update && apt-get install -y \
    bnfc \
    cup \
    jlex \
    make \
    openjdk-11-jdk \
    python3
ENV CLASSPATH=.:/usr/share/java/JLex.jar:/usr/share/java/cup.jar:/build_root

COPY ./src .

# Generate parser generator files and abstract syntax classes from grammar
RUN bnfc --java NotC.cf && \
    java JLex.Main NotC/Yylex && \
    java java_cup.Main -destdir NotC NotC/NotC.cup

# Patch abstract syntax for expressions with type annotations
RUN sed -i 's/class Exp/class Exp extends TypeAnnotatedNode/' NotC/Absyn/Exp.java

# Build all sources
RUN javac -d .. $(find NotC -name "*.java") 


# Run tests
WORKDIR /build_root/tests
COPY ./tests .
RUN python3 run_tests.py


FROM openjdk:11-jre-slim
WORKDIR compiler_root
COPY --from=build /build_root/NotC ./NotC
COPY --from=build /usr/share/java/cup.jar /usr/share/java/cup.jar
ENV CLASSPATH=.:/usr/share/java/cup.jar

ENTRYPOINT ["java", "NotC.Compiler"]
