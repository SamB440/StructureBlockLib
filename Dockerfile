FROM openjdk:8

WORKDIR /minecraft

COPY ["structureblocklib-tools", "structureblocklib-bukkit-sample/build/libs", "./"]

RUN wget "https://hub.spigotmc.org/jenkins/job/BuildTools/lastStableBuild/artifact/target/BuildTools.jar" && \
    java -Xmx1024M -jar BuildTools.jar --rev "1.15" && \
    echo "eula=true" > eula.txt

CMD rm -r plugins/PetBlocks -f && \
    rm -r world -f && \
    rm -r world_nether -f && \
    rm -r world_the_end -f && \
    cp -r world-1.14/world . && \
    java -Xmx1024M -Xmx1024M -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 -jar spigot-1.15.jar