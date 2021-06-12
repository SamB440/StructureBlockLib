# This docker file uses multi-stage builds.
# 1. Resolve minecraft-dependencies for 1.8 - 1.16 with jdk8
FROM openjdk:8 AS dependencies-jdk8
WORKDIR /tmp
RUN apt-get update
RUN apt-get install maven -y
RUN wget "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
RUN java -jar BuildTools.jar --rev 1.9.4
RUN java -jar BuildTools.jar --rev 1.10
RUN java -jar BuildTools.jar --rev 1.11
RUN java -jar BuildTools.jar --rev 1.12
RUN java -jar BuildTools.jar --rev 1.16.4
RUN java -jar BuildTools.jar --rev 1.13.2
RUN java -jar BuildTools.jar --rev 1.14.4
RUN java -jar BuildTools.jar --rev 1.15
RUN java -jar BuildTools.jar --rev 1.16.4
CMD ["sh","-c","/bin/bash"]

# 2. Resolve minecraft-dependencies for 1.17 - latest with jdk16
FROM adoptopenjdk/openjdk16 AS dependencies-jdk16
WORKDIR /tmp
RUN apt-get update
RUN apt-get install maven -y
RUN apt-get install wget -y
RUN apt-get install git -y
RUN wget "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar"
RUN java -jar BuildTools.jar --rev 1.17 --remapped

# 3. Build plugin for 1.8 - 1.17 with jdk16
FROM adoptopenjdk/openjdk16 AS plugin-jdk16
WORKDIR /tmp
RUN apt-get update
RUN apt-get install maven -y
RUN apt-get install dos2unix -y
COPY --from=dependencies-jdk8 /root/.m2/repository/org/spigotmc /root/.m2/repository/org/spigotmc/
COPY --from=dependencies-jdk16 /root/.m2/repository/org/spigotmc /root/.m2/repository/org/spigotmc/
COPY . /tmp
RUN chmod +x gradlew
RUN dos2unix gradlew
RUN ./gradlew build pluginJar --no-daemon

# 4. Launch a minecraft server with jdk16 and plugin
FROM adoptopenjdk/openjdk16
# Change to the current plugin version present in build.gradle
ENV PLUGIN_VERSION=2.2.1
# Change to the server version you want to test.
ENV SERVER_VERSION=1.17-R0.1-SNAPSHOT/spigot-1.17-R0.1-SNAPSHOT.jar
# Port of the Minecraft Server.
EXPOSE 25565
# Port for Remote Debugging
EXPOSE 5005
WORKDIR /app
RUN apt-get update
RUN echo "eula=true" > eula.txt && mkdir plugins
COPY ./structureblocklib-tools/world-1.14 /app/
COPY ./structureblocklib-tools/ops.json /app/
COPY --from=dependencies-jdk16 /root/.m2/repository/org/spigotmc/spigot/$SERVER_VERSION /app/spigot.jar
COPY --from=plugin-jdk16 /tmp/structureblocklib-bukkit-sample/build/libs/structureblocklib-bukkit-sample-$PLUGIN_VERSION.jar /app/plugins/Structureblocklib.jar
CMD ["sh","-c","java -DIReallyKnowWhatIAmDoingISwear -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar spigot.jar"]
