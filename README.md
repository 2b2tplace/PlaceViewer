# PlaceViewer
An [Ignite](https://github.com/vectrix-space/ignite) mod for [Paper](https://papermc.io/) 1.21.4 to allow 
viewing [zvcr](https://github.com/2b2tplace/zvcr) regions in-game immediately, for any timestamp, acting like 
a 3D [Wayback Machine](https://web.archive.org/) for Minecraft servers, primarily 2b2t.

### How it works
PlaceViewer uses Java's native interface to read zvcr region files from disk using the C++ zvcr library, then 
generates chunk packets for any given chunk a player requests ingame. The world is never actually loaded into the
server and always remains empty. This mod additionally entirely disables the vanilla use of server region files
(.mca), and they are never read or written.

This is also why a regular Bukkit/Spigot/Paper plugin would not have been enough. Changing core mechanics of the
region/chunk loading logic is not something you can achieve without making a server fork or using mixins.

> ### Official 2b2t.place Instance
> We host an official instance of PlaceViewer for free, allowing anyone to access our 1M² world download of 2b2t.org. \
> Server IP: `wayback.2b2t.place` (Minecraft Java Edition, 1.21.4, as of writing)

# Command Usage
Commands accept parameters in sequence, denoted by `<argument>`. Optional parameters are denoted by `[argument]`.

- `/ignore <player>`, `/i <player>`: Ignore all chat messages sent by a player.
- `/dimension <dimension>`, `/dim <dimension>`: Switches your current dimension.
- `/flashback`, `/fb`: Select or browse history of your current location. When executed without arguments, a
  clickable interface will be displayed in the chat instead.
    - Browse: `/flashback browse [year] [month] [day]`
    - Select: `/flashback select <unix-timestamp-millis>`
- `/now`: Sets your map view mode back to the latest snapshot.
- `/help`, `/?`: Shows the PlaceViewer help page.
- `/whisper`, `/w`, `/tell`, `/t`, `/msg`: Send a private message to a player.
    - Usage: `/whisper <player> <message>` 
- `/reply <message>`, `/r <message>`: Send a message to the player that last talked to you.
- `/lastmsg <message>`, `/last <message>`, `/l <message>`: Send a message to the player you last talked to.
- `/teleport`, `/tp`: Teleports you to a selected position.
    - Usage: `/teleport <x> <y> <z> [dimension]`
    - Usage: `/teleport <x> <z> [dimension]`
    - `<dimension>` must be `overworld`, `nether`, or `the_end`
    - `<coordinate>` can be set to `~` to retain the coordinate of the current position   

# Building the Mod from Source
> Note: Windows is currently not officially supported. It will probably compile, however no guarantees are made on
  functionality.

This project is written in Java 21 and C++23, and uses CMake and Gradle. With that in mind, it consists out of a shared 
libary, and a Java component for the Ignite mod.

### Dependencies
#### Debian/Ubuntu
```sh
apt install cmake gcc-14 g++-14 clang-20 libboost-iostreams-dev openjdk-21-jdk
```

### Clone & Build
> Note: These instructions WILL change very soon; this entire build process can be automated in gradle directly.
```sh
git clone https://github.com/2b2tplace/PlaceViewer.git

# build the shared library first (C++)
cd PlaceViewer/cpp/PlaceViewer

# choosing clang in this example. you may choose a different compiler if you need
cmake -S . -B build -DCMAKE_BUILD_TYPE=Release -DCMAKE_C_COMPILER=clang-20 -DCMAKE_CXX_COMPILER=clang++-20

# executable should appear at ./build/libPlaceViewer-{arch}.{so/dylib/dll} (value of arch determined by `uname -m`)
# assuming linux, arch = x86_64 for this example
cmake --build build --target PlaceViewer --parallel

# zip shared libraries to include in the jar
cd ../..
mv cpp/PlaceViewer/libPlaceViewer-x86_64.so src/main/resources/
cd src/main/resources
zip libraries.zip libPlaceViewer-x86_64.so
rm libPlaceViewer-x86_64.so

# build the java component second (Java)
# assuming linux in this example; use gradlew.bat on windows
# final ignite mod jar will appear at build/libs/placeviewer-{version}.jar
cd ../../..
./gradlew paperweightUserdevSetup
./gradlew build
```

# Server Setup
> Ignite server setup taken from [the official repo](https://github.com/vectrix-space/ignite#install).

To set up a Minecraft server with Ignite + Paper, you will need the `ignite.jar` and a `paper.jar` in the same
directory. Download the v1.2.1 `ignite.jar` from their
[releases page](https://github.com/vectrix-space/ignite/releases/#release-v1.2.1) and download Paper 1.21.4 from
the [official download page (build explorer)](https://fill-ui.papermc.io/projects/paper/version/1.21.4). You can simply
rename the Paper server jar to `paper.jar`, and drop it in a directory to host your server in.

Create a directory named `mods` in the same location as your two server jars, and copy the `placeviewer-{version}.jar`
in there.

Run the `ignite.jar` as if it were a regular Paper server jar. It is recommended to use 
[Aikar's flags](https://docs.papermc.io/paper/aikars-flags/) for the run command (adjust memory min/max for your system):
```sh
java -Xms4096M -Xmx4096M -XX:+AlwaysPreTouch -XX:+DisableExplicitGC -XX:+ParallelRefProcEnabled \
  -XX:+PerfDisableSharedMem -XX:+UnlockExperimentalVMOptions -XX:+UseG1GC -XX:G1HeapRegionSize=8M \
  -XX:G1HeapWastePercent=5 -XX:G1MaxNewSizePercent=40 -XX:G1MixedGCCountTarget=4 -XX:G1MixedGCLiveThresholdPercent=90 \
  -XX:G1NewSizePercent=30 -XX:G1RSetUpdatingPauseTimePercent=5 -XX:G1ReservePercent=20 \
  -XX:InitiatingHeapOccupancyPercent=15 -XX:MaxGCPauseMillis=200 -XX:MaxTenuringThreshold=1 -XX:SurvivorRatio=32 \
  -Dusing.aikars.flags=https://mcflags.emc.gs -Daikars.new.flags=true -jar \
  ignite.jar --nogui
```

As usual with Minecraft servers, you will have to agree to the EULA (edit the eula.txt file), then re-run with the same
command. 

When running with PlaceViewer installed for the first time, it should fail and immediately shut down, with an error
something like this:
```
[13:12:52 ERROR]: Encountered an unexpected exception
java.lang.IllegalStateException: Unable to load registries, shutting down server.
```

## PlaceViewer setup
After running the server for the first time, a configuration file named `placeviewer.yml` in your Minecraft server's
root directory should have been created. The two important lines to change are here:
```yml
paths:
    zvcr-parent-directory: 'zvcr3'
    registry-directory: 'registries'
```

The `zvcr-parent-directory` field should be replaced with the path to your
[zvcr directory](https://github.com/2b2tplace/zvcr#zvcr-directory-structure-specification) containing all region files
for all three dimensions. For registries, clone the [mc-cpp repo](https://github.com/2b2tplace/mc-cpp) and set the
`registries` value in the configuration to the path of the 
[registries directory](https://github.com/2b2tplace/mc-cpp/tree/main/registries) in the cloned repo on your system.

```sh
git clone https://github.com/2b2tplace/mc-cpp
# registries are at `./mc-cpp/registries`
```

> Assuming you are here from the 1M² world download self-hosting setup:
> - If you used the `mount.sh` script to mount as a read-only filesystem, use the same path for `zvcr-parent-directory`
>   as what was used for the `-t` argument passed into that script. By default, this would be `/mnt/wdl`.
> - If you used the `extract.sh` script, use the same path for `zvcr-parent-directory`
    as what was used for the `-t` argument passed into that script. This is be the directory you extracted the zvcr 
    files into.

Lastly, you should absolutely change these values in `server.properties`:
```properties
allow-flight=true
force-gamemode=true
gamemode=spectator
spawn-monsters=false
```

The server should now successfully run with these changes and let you view the zvcr world in-game. You can change the
rest of the configuration as well if you'd like.
