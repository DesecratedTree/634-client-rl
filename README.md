# runetek5-634

[![Build][badge-ci]][ci]
[![Java 21 to 25][badge-java]][java]
[![Void compatible][badge-void]][void]

The game client for [Void][void], a RuneScape private server. It is the RuneScape client from
December 2010 (build 634), with a launcher that connects to any Void server and a few optional
quality-of-life extras.

## Features

- **Ready to play.** It connects to [2011Scape](https://2011.rs) (`play.2011.rs`) out of the box.
- **Any server.** The launcher takes a host, a port and the server's keys.
- **Extras**, each with an option to turn it on or off:
  - Tile markers: right-click a tile and choose *Mark tile* to outline it.
  - Experience drops: a "+N Skill" drop rises near the minimap when you gain experience.
  - NPC labels: names and combat levels above NPCs' heads.
  - Ground item labels: the names of the items lying on each tile.
  - Shift and left-click to drop inventory items (on by default).
  - A wider mouse-wheel zoom and a longer render distance (on by default).
  - Up to 144 frames a second and a window scaled to your screen (by default), and sharper edges
    in OpenGL (MSAA, SMAA, supersampling).
  - Developer tools: item, NPC and object ids on right-click menus, coordinates on screen, and a
    better command console.

## Requirements

- **Java 21 to 25.** Java 26 and later may not run it: the 2010 client is built on the Applet API,
  which newer JDKs are removing. The launcher says so if your Java lacks it.
- **On Windows, a 32-bit (x86) Java for DirectX.** The game's DirectX library only comes in 32-bit,
  so a 64-bit Java gets OpenGL and software mode only. [Liberica][liberica] still publishes 32-bit
  Windows builds of Java 21.
- **Nothing else to play on 2011Scape.** To run your own server, see the [Void repository][void],
  and start the client with `--address localhost`.

On Linux the launcher puts the client's native libraries on the library path itself, so OpenGL
works however you start it.

## Download

Get the latest jar from the [releases page][releases]:

| File | What it is |
|---|---|
| `runetek5-634-<version>.jar` | The client. |
| `SHA256SUMS` | Checksums to verify the download. |

Double-click the jar, or start it from a terminal with any of the [options](#options):

```bash
java -jar runetek5-634-2.1.0.jar --tile-markers --xp-drops --npc-labels
```

## Options

With no options the client starts with these on: up to 144 frames a second, twice the render
distance, the wider zoom, the better console and shift-drop, and a scale that suits the screen
(1.5 from 1080p, 2 at 4K). Each option below overrides its default; the other extras are off until
you turn them on. Most can also be turned on and off in game with the command shown.

**Connecting**

| Option | What it does |
|---|---|
| `--address <host>` | The server to connect to. The default is `play.2011.rs`; `localhost` is a server on this computer. |
| `--port <port>` | The server's port, if it does not use Void's 43594. |
| `--keys <file>` | Read the server's keys from its `game.properties` (Void keeps it in `game/src/main/resources`). |
| `--login-modulus <hex>`, `--js5-modulus <hex>` | Give the server's keys in hex instead. |
| `--server <void\|jagex>` | The server's protocol. `void` (the default) suits a Void server; `jagex` restores the 2010 client's: login through the lobby, and the original packet sizes and scripts. |
| `--packets <file>` | Override server packet sizes, one `<opcode>=<size>` per line (`-1` and `-2` for a size byte or short). |
| `--world <id>` | The world. The default is 1. |
| `--lang <english\|german>` | The language. The default is English. |

The client has the keys of a standard Void server built in, so the key options are only needed
for a server that changed them.

**Gameplay extras**

| Option | What it does | In game |
|---|---|---|
| `--tile-markers` | Right-click a tile and choose *Mark tile* to outline it; markers stay until unmarked. | `::tilemarkers` |
| `--xp-drops` | A "+N Skill" drop rises and fades when you gain experience. | `::xpdrops` |
| `--npc-labels` | NPC names, with their combat level, above their heads. | `::npclabels` |
| `--ground-items` | The names (and counts) of the items on each tile, above it. | `::grounditems` |
| `--no-shift-drop` | Turn off dropping an item with Shift and a left click (on by default). | `::shiftdrop` |
| `--no-zoom` | Keep the mouse wheel to the original zoom range (the wider range is on by default). | |
| `--zoom-key <key>` | The key held while the wheel zooms the camera: `ctrl` (default), `alt`, `shift` or `none`. | |
| `--render-distance <x>` | Draw the world x times further away. The default is 2; `1` is the original distance. | |

**Graphics**

| Option | What it does | In game |
|---|---|---|
| `--fps <n>` | Draw up to n frames a second (default 144). The game keeps its speed; the camera, players, NPCs and animations move smoothly between game cycles. | |
| `--scale <x>` | Draw the game x times larger. By default it follows the screen: 1.5 from 1080p, 2 at 4K, otherwise 1, measured as the desktop sizes it, so a display your system already enlarges is not scaled twice. `--scale 1` keeps the original size. Works with software, OpenGL, DirectX and safe mode; the native OpenGL renderer keeps the game at its own size. | |
| `--msaa <n>` | OpenGL: anti-alias the 3D view with n samples (0, 2, 4 or 8) in a framebuffer of its own, so the interface stays sharp. Cut-out textures such as leaves get alpha-to-coverage (`--no-a2c` turns that off). | `::msaa n` |
| `--smaa` | OpenGL: smooth the 3D view's edges with SMAA 1x, with or without MSAA. | `::smaa` |
| `--render-scale <x>` | OpenGL: draw the 3D view 1.5 or 2 times larger and shrink it (supersampling). | `::renderscale x` |

**For developers**

| Option | What it does |
|---|---|
| `--debug` | Show ids and tile coordinates on right-click menus, and your coordinates under `::displayfps`. |
| `--no-console` | Turn off the console's command history, word editing and tab completion (on by default). |
| `--dev` | `--debug` with the wider zoom and the better console. |

## Running from source

Clone the repository and start the client; it connects to `play.2011.rs`:

```bash
git clone https://github.com/2011Scape/634-client.git
cd 634-client
./gradlew run
```

On Windows, use `gradlew.bat` instead of `./gradlew`. Give options with `--args`:

```bash
./gradlew run --args="--address localhost"
./gradlew run --args="--address play.example.org --keys ~/void/game/src/main/resources/game.properties"
./gradlew run --args="--tile-markers --npc-labels --ground-items"
```

### In an IDE

Open the folder as a Gradle project with a Java 21 SDK. Then either run the Gradle task
`application → run`, or make an *Application* run configuration with main class `RuneTek5`, the
`runetek5-634.main` classpath, the options as program arguments, and these VM options:

```
--add-opens java.base/java.lang=ALL-UNNAMED --enable-native-access=ALL-UNNAMED -Xmx256m
```

## Building a standalone client

`./gradlew release` builds the release jar into `build/release`.

For a folder with start scripts instead, run `./gradlew installDist`. The client is then in
`build/install/runetek5-634`: start it with `bin/runetek5-634`, or `bin\runetek5-634.bat` on
Windows, with the same options as above. Copy the whole folder to share it; the person running it
needs Java 21 to 25 installed.

## For developers

The client is written in Kotlin, as a Kotlin Multiplatform project with a JVM and a browser target:
the game is in `src/commonMain/kotlin`, on shims of the Java APIs it uses, the JVM side (the native
classes, the launcher `RuneTek5`) in `src/jvmMain/kotlin`, and the browser side in `src/jsMain/kotlin`. It is a straight conversion of the deobfuscated
Java, which keeps the original's structure, names and annotations, and the names and signatures the
native libraries bind to. Code added to the
2010 client is marked with a comment that starts "Not part of the original client".

- **Adding a feature:** start with [`docs/FEATURES.md`](docs/FEATURES.md), which says where a
  feature hooks in, how it draws, and how to try it.
- **Where the code is:** [`docs/PACKAGES.md`](docs/PACKAGES.md), including the native bindings
  that must never change.
- **How each part works:**
  - [`docs/CS2.md`](docs/CS2.md): interfaces and client scripts.
  - [`docs/NETWORK.md`](docs/NETWORK.md): server and client packets.
  - [`docs/ENTITIES.md`](docs/ENTITIES.md): players, NPCs and update masks.
  - [`docs/CONFIG.md`](docs/CONFIG.md): items, NPCs, locations and the other config types.
  - [`docs/CACHE.md`](docs/CACHE.md): the disk cache and JS5.
  - [`docs/RENDERING.md`](docs/RENDERING.md): the toolkits and how a frame is drawn.
  - [`docs/CLIENT.md`](docs/CLIENT.md): the main loop, map building, scene and sound.
- **Trying a change:** `./gradlew runClient` starts the client itself against a Void server on
  this computer, with `-Pclient.*` switches such as `-Pclient.debug=true`.
- **The browser client:** the same game runs in a browser through a WebSocket relay
  (`node tools/relay.mjs`), served by `node tools/serve.mjs`; [below](#running-the-browser-client)
  says how to build, serve and open it. A release includes the browser bundle as a zip beside the jar.
- **Checking a change:** `./gradlew check` (what CI runs, with no server needed) loads and verifies
  every class from the release jar, and fails on a method over HotSpot's 8000-byte JIT limit unless
  `config/huge-methods.txt` lists it; `./gradlew build` also builds the browser target, which fails
  if the common code uses a JVM API. The in-client smoke test (`-Pclient.test=smoke`, see `SmokeTest`)
  needs a Void server; in the browser it runs from the page address (`?client.test=smoke`).

### Running the browser client

1. A Void server on this computer, as for the desktop smoke test.
2. The relay, from WebSockets to the server's TCP port: `node tools/relay.mjs` (no dependencies;
   arguments `[listenPort=8081] [serverHost=localhost] [serverPort=43594]`), or
   `websockify 8081 localhost:43594`. The client connects to `ws://localhost:8081/proxy`, or to
   `window.CONFIG.url` when the page sets it.
3. `./gradlew jsBrowserDevelopmentWebpack`, then serve it: `node tools/serve.mjs` (no
   dependencies; arguments `[port=8080] [bundleDir] [referenceDir=~/634-smoke-ref/browser]
   [shotDir=build/smoke-browser]`), and open `http://localhost:8080/`. It never lets the browser
   cache, so a rebuilt bundle is picked up on the next reload. `./gradlew build` makes the minified
   bundle in `build/dist/js/productionExecutable`, and `./gradlew release` zips it beside the jar.
4. The first visit downloads the cache into IndexedDB; later visits load it from there. The client
   starts as the launcher does (`LauncherDefaults`), but always at render distance 1, on the toolkit the game's settings
   name, which it keeps across reloads. The tutorial hides the chat box, so commands go through the developer
   console (the `` ` `` key): `tk1` switches to the Java OpenGL toolkit on WebGL 2, `tk2` to the
   native software toolkit's browser version, and `tele 3222 3218` takes an admin player to
   Lumbridge (so does ctrl-clicking the minimap). Switches go in the address (`?client.msaa=4`) or a
   hosted page's `window.CONFIG`. `window.__log` holds the last 500 console lines. Leaving the page
   closes the client's connections, so the server logs the player out at once.
5. The smoke test: `http://localhost:8080/?client.test=smoke&client.test.toolkit=1` logs in as
   `smoketest`, runs the desktop test's checks without the launcher's defaults, compares its frames
   with `~/634-smoke-ref/browser` (served at `/smoke-ref/`), uploads them to `build/smoke-browser`,
   and puts SMOKE PASS or FAIL on the console, in `window.__log`, in `#smoke-result` and in the page
   title. It and the desktop smoke test both log in as `smoketest`: run one at a time.

## Credits

- [Void][void] and [GregHib/void-client][void-client], for the server, and for the client patches
  and extras this client builds on.
- [OpenRS2][openrs2], for the deobfuscator and the [archive][archive] of the original client.
- [RuneWiki][runewiki], for deobfuscating build 634.
- [StrongHold/runescape-667][667], [LostCityRS/RS910][rs910], [Pazaz/RT4-Client][rt4] and [Pazaz/RT5-Client][rt5], for the names given to the code.
- [FabricMC Matcher][matcher], for matching the code between builds.
- [SMAA](https://github.com/iryoku/smaa) by Jorge Jimenez, Jose I. Echevarria, Belen Masia, Fernando Navarro and
  Diego Gutierrez, for the SMAA shader and lookup textures (MIT licence, in `src/jvmMain/resources/smaa/`).

## Disclaimer

This project is not affiliated with or endorsed by Jagex Ltd. RuneScape is a trademark of
Jagex Ltd.

[badge-ci]: https://github.com/2011Scape/634-client/actions/workflows/build.yml/badge.svg
[badge-java]: https://img.shields.io/badge/Java-21_to_25-ED8B00?logo=openjdk&logoColor=white
[badge-void]: https://img.shields.io/badge/Void-compatible-2EA44F
[ci]: https://github.com/2011Scape/634-client/actions/workflows/build.yml
[releases]: https://github.com/2011Scape/634-client/releases
[java]: https://adoptium.net/temurin/releases/?version=21
[liberica]: https://bell-sw.com/pages/downloads/#jdk-21-lts
[void]: https://github.com/GregHib/void
[void-client]: https://github.com/GregHib/void-client
[openrs2]: https://github.com/openrs2/openrs2
[archive]: https://archive.openrs2.org/clients
[runewiki]: https://github.com/RuneWiki/openrs2-nonfree
[667]: https://github.com/StrongHold/runescape-667
[rt4]: https://github.com/Pazaz/RT4-Client
[rt5]: https://github.com/Pazaz/RT5-Client
[rs910]: https://github.com/LostCityRS/RS910
[matcher]: https://github.com/FabricMC/Matcher
