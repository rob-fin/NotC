FROM bitnami/minideb
WORKDIR /compiler_root
RUN apt-get update
RUN apt-get install -y openjdk-11-jdk
RUN apt-get install -y bnfc
RUN apt-get install -y cup
RUN apt-get install -y jlex
ENV CLASSPATH=.:/usr/share/java/JLex.jar:/usr/share/java/cup.jar
RUN apt-get install -y make
COPY NotC.cf .
RUN bnfc --java -m NotC.cf
RUN make
