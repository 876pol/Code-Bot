FROM openjdk:11
WORKDIR /
RUN apt update && apt install -y python2 python3 build-essential nodejs
ARG jarpath="/build/libs/Code Bot.jar"
ARG jar="Code Bot.jar"
ADD ${jarpath} ${jar}
CMD java -jar "Code Bot.jar"