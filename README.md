# PlaceViewer
An [Ignite](https://github.com/vectrix-space/ignite) mod for [Paper](https://papermc.io/) 1.21.4 to allow viewing [zvcr](https://github.com/2b2tplace/zvcr) regions in-game immediately, for any timestamp, acting like a 3D [Wayback Machine](https://web.archive.org/) for Minecraft servers, primarily 2b2t.

## Information
PlaceViewer uses Java's native interface to read zvcr region files from disk using the C++ zvcr library, then generates chunk packets for any given chunk a player requests ingame. 
The world is never actually loaded into the server and always remains empty. 
This mod additionally entirely disables the vanilla use of server region files (.mca), and they are never read or written.
