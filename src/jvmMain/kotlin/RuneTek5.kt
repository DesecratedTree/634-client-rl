import com.jagex.game.runetek5.client.features.LauncherDefaults
import com.jagex.game.runetek5.client.login.LoginManager
import com.jagex.game.runetek5.js5.request.RequestManager
import java.awt.GraphicsEnvironment
import java.io.IOException
import java.io.Reader
import java.lang.management.ManagementFactory
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList
import java.util.Arrays
import java.util.Properties
import java.util.regex.Pattern

/**
 * Starts the client against a server of your choosing.
 *
 * The client's own `main` takes six positional arguments, always connects to this machine and
 * has the server's public keys built in. This launcher sets the `client.*` switches that
 * [ClientFeatures] reads, installs the keys given here, and then hands over to that `main`,
 * so the client still runs in its own frame with its disk cache.
 * ```
 * --address <host>            the server to connect to (default play.2011.rs; localhost for a server on
 *                             this machine)
 * --port <port>               the port for both connection attempts (default 43594, Void's)
 * --keys <game.properties>    read security.game.modulus and security.file.modulus from a Void config
 * --login-modulus <hex>       the login RSA modulus
 * --js5-modulus <hex>         the file server RSA modulus
 * --server <void|jagex>       the server's protocol: Void's (default) or the 2010 client's
 * --packets <file>            server packet sizes to override, one <opcode>=<size> per line
 * --world <id>                the world id (default 1)
 * --lang <english|german>     the language (default english)
 * --debug                     ids and coordinates on menus and in the fps overlay
 * --zoom, --no-zoom          a wider mouse-wheel zoom (default on)
 * --zoom-key <key>            the key held for the wheel to zoom: ctrl (default), alt, shift or none
 * --render-distance <x>       draw and fog distance times x (default 2)
 * --fps <n>                   frames drawn per second (default 144); the game logic stays at 50 per second
 * --scale <x>                 window pixels per game pixel (default from the screen: 1, 1.5 from 1080 lines
 *                             high, 2 from 2160)
 * --console, --no-console     command history, word editing and autocomplete in the console (default on)
 * --shift-drop, --no-shift-drop  Shift with a left click drops an inventory item (default on; ::shiftdrop
 *                             toggles it)
 * --tile-markers              mark tiles from the right-click menu ("Mark tile"; ::tilemarkers toggles it)
 * --xp-drops                  "+N Skill" drops when experience goes up (::xpdrops toggles it)
 * --npc-labels                NPC names and combat levels above their heads (::npclabels toggles it)
 * --ground-items              the names of items on the ground above their tiles (::grounditems toggles it)
 * --msaa <n>                  OpenGL: draw the 3D view in its own framebuffer with n samples (0, 2, 4, 8)
 * --smaa                      OpenGL: smooth the 3D view's edges with SMAA 1x as well
 * --render-scale <x>          OpenGL: draw the 3D view x times larger and shrink it (1.0, 1.5, 2.0)
 * --no-a2c                    OpenGL: no alpha-to-coverage for cut-out textures
 * --dev                       --debug, --zoom and --console together
 * ```
 * On Linux the window class of a Java application is the simple name of its main class, and desktops
 * match windows to installed applications by it. This class is named after the engine rather than
 * something as common as "Launcher", which other applications claim.
 */
object RuneTek5 {

    /** The server the launcher connects to when no --address is given. */
    private const val DEFAULT_ADDRESS: String = "play.2011.rs"

    /** The port a Void server listens on. */
    private const val DEFAULT_PORT: Int = 43594

    @JvmStatic
    @Throws(IOException::class)
    fun main(args: Array<String>) {
        // The client is an applet (GameShell extends java.applet.Applet), so a JDK without the
        // Applet API cannot run it: say so rather than fail on the first class that needs it.
        try {
            Class.forName("java.applet.Applet")
        } catch (ex: ClassNotFoundException) {
            System.err.println("This client needs Java 21 to 25. Java " + Runtime.version().feature()
                + " no longer has the Applet API the 2010 client is built on.")
            System.exit(1)
        }
        if (relaunchWithNativeLibraryPath(args)) {
            return
        }
        var world = "1"
        var lang = "english"
        var loginModulus: BigInteger? = null
        var js5Modulus: BigInteger? = null

        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when (arg) {
                "--address" -> System.setProperty("client.address", value(args, ++i, arg))
                "--port" -> System.setProperty("client.port", Integer.toString(Integer.parseInt(value(args, ++i, arg))))
                "--server" -> System.setProperty("client.server", value(args, ++i, arg))
                "--packets" -> System.setProperty("client.packets", path(value(args, ++i, arg)).toString())
                "--world" -> world = Integer.toString(Integer.parseInt(value(args, ++i, arg)))
                "--lang" -> lang = value(args, ++i, arg)
                "--scale" -> System.setProperty("client.scale", java.lang.Double.toString(java.lang.Double.parseDouble(value(args, ++i, arg))))
                "--fps" -> System.setProperty("client.fps", Integer.toString(Integer.parseInt(value(args, ++i, arg))))
                "--render-distance" -> System.setProperty("client.renderDistance", java.lang.Double.toString(java.lang.Double.parseDouble(value(args, ++i, arg))))
                "--debug" -> System.setProperty("client.debug", "true")
                "--zoom" -> System.setProperty("client.zoom", "true")
                "--no-zoom" -> System.setProperty("client.zoom", "false")
                "--zoom-key" -> System.setProperty("client.zoomKey", value(args, ++i, arg))
                "--console" -> System.setProperty("client.console", "true")
                "--no-console" -> System.setProperty("client.console", "false")
                "--shift-drop" -> System.setProperty("client.shiftDrop", "true")
                "--no-shift-drop" -> System.setProperty("client.shiftDrop", "false")
                "--tile-markers" -> System.setProperty("client.tileMarkers", "true")
                "--xp-drops" -> System.setProperty("client.xpDrops", "true")
                "--npc-labels" -> System.setProperty("client.npcLabels", "true")
                "--ground-items" -> System.setProperty("client.groundItemLabels", "true")
                "--msaa" -> System.setProperty("client.msaa", Integer.toString(Integer.parseInt(value(args, ++i, arg))))
                "--smaa" -> System.setProperty("client.smaa", "true")
                "--render-scale" -> System.setProperty("client.renderScale", java.lang.Double.toString(java.lang.Double.parseDouble(value(args, ++i, arg))))
                "--no-a2c" -> System.setProperty("client.a2c", "false")
                "--dev" -> {
                    System.setProperty("client.debug", "true")
                    System.setProperty("client.zoom", "true")
                    System.setProperty("client.console", "true")
                }
                "--login-modulus" -> loginModulus = BigInteger(value(args, ++i, arg), 16)
                "--js5-modulus" -> js5Modulus = BigInteger(value(args, ++i, arg), 16)
                "--keys" -> {
                    val keys = readKeys(path(value(args, ++i, arg)))
                    loginModulus = keys[0]
                    js5Modulus = keys[1]
                }
                "--help", "-h" -> {
                    usage()
                    return
                }
                else -> {
                    System.err.println("Unknown option $arg")
                    usage()
                    System.exit(1)
                }
            }
            i++
        }

        // Without a port the client would work one out from the world id, which a Void server does not
        // listen on, so the launcher defaults to Void's port.
        if (System.getProperty("client.port") == null) {
            System.setProperty("client.port", Integer.toString(DEFAULT_PORT))
        }
        setDefault("client.address", DEFAULT_ADDRESS)

        // The launcher's defaults (shared with the browser page), each kept only when no option set it.
        LauncherDefaults.apply(screenHeight())

        // ClientFeatures reads the switches when the client first uses it, which is after this point.
        if (loginModulus != null) {
            LoginManager.rsaModulus = BigInt(loginModulus.toString(16), 16)
        }
        if (js5Modulus != null) {
            RequestManager.js5Modulus = BigInt(js5Modulus.toString(16), 16)
        }

        client.main(arrayOf<String?>(world, world, "0", "live", lang, "game0"))
    }

    /**
     * On Linux the native OpenGL toolkit (libhw3d.so) links against libjaclib.so by name, and the
     * system's loader finds it only on LD_LIBRARY_PATH, which it reads once when the JVM starts.
     * Without the client's native library directory there, the client falls back to the software
     * toolkit. So when the directory is missing, start this launcher again in a JVM that has it, with
     * the same JVM options and arguments, and exit with its status. Returns false when nothing was
     * needed. -Dclient.nativeDir overrides the directory; -Dclient.relaunch=false turns this off (a
     * debugger then needs LD_LIBRARY_PATH set by hand).
     */
    @Throws(IOException::class)
    private fun relaunchWithNativeLibraryPath(args: Array<String>): Boolean {
        if (!System.getProperty("os.name").startsWith("Linux") || "false" == System.getProperty("client.relaunch")
            || System.getenv("RUNETEK5_RELAUNCHED") != null) {
            return false
        }
        val dir = System.getProperty("client.nativeDir", System.getProperty("user.home") + "/.jagex_cache_32/runescape")
        val current: String? = System.getenv("LD_LIBRARY_PATH")
        // Pattern.split is what String.split(":") does (trailing empty strings dropped)
        if (current != null && Arrays.asList(*Pattern.compile(":").split(current)).contains(dir)) {
            return false
        }

        val command = ArrayList<String>()
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString())
        for (option in ManagementFactory.getRuntimeMXBean().inputArguments) {
            // a debugger agent would try to bind the port this JVM already holds
            if (!option.startsWith("-agentlib:jdwp") && !option.startsWith("-Xrunjdwp")) {
                command.add(option)
            }
        }
        // the release jar's manifest opens java.lang, which a JVM started by -cp does not read
        command.add("--add-opens")
        command.add("java.base/java.lang=ALL-UNNAMED")
        command.add("-cp")
        command.add(System.getProperty("java.class.path"))
        command.add(RuneTek5::class.java.name)
        command.addAll(Arrays.asList(*args))

        val builder = ProcessBuilder(command).inheritIO()
        builder.environment()["LD_LIBRARY_PATH"] = if (current == null || current.isEmpty()) dir else "$dir:$current"
        builder.environment()["RUNETEK5_RELAUNCHED"] = "1"
        try {
            System.exit(builder.start().waitFor())
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            System.exit(1)
        }
        return true
    }

    private fun setDefault(name: String, value: String) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, value)
        }
    }

    /** The height of the screen the client opens on, in the desktop's own units; 0 when headless. */
    private fun screenHeight(): Int {
        if (GraphicsEnvironment.isHeadless()) {
            return 0
        }
        return GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration.bounds.height
    }

    private fun value(args: Array<String>, index: Int, option: String): String {
        if (index >= args.size) {
            System.err.println("$option needs a value")
            usage()
            System.exit(1)
        }
        return args[index]
    }

    /**
     * A path from the command line. Gradle passes `--args` without a shell, so a leading
     * `~` is expanded here. A relative path is taken from the module's directory, where Gradle
     * runs the client.
     */
    private fun path(value: String): Path {
        var value = value
        if (value == "~" || value.startsWith("~/")) {
            value = System.getProperty("user.home") + value.substring(1)
        }
        return Path.of(value)
    }

    /** The login and file server moduli from a Void game.properties, in that order. */
    @Throws(IOException::class)
    private fun readKeys(file: Path): List<BigInteger> {
        val properties = Properties()
        Files.newBufferedReader(file).use { reader: Reader ->
            properties.load(reader)
        }
        val keys = ArrayList<BigInteger>()
        for (name in listOf("security.game.modulus", "security.file.modulus")) {
            val hex = properties.getProperty(name)
            if (hex == null) {
                throw IOException("$file has no $name")
            }
            keys.add(BigInteger((hex as java.lang.String).strip(), 16))
        }
        return keys
    }

    private fun usage() {
        System.err.println("""
            usage: RuneTek5 [--address host] [--port port] [--keys game.properties]
                            [--login-modulus hex] [--js5-modulus hex] [--server void|jagex] [--packets file]
                            [--world id] [--lang english|german]
                            [--debug] [--[no-]zoom] [--zoom-key key] [--render-distance x] [--fps n] [--scale x] [--msaa n] [--smaa] [--render-scale x] [--no-a2c] [--[no-]console] [--[no-]shift-drop] [--dev]
                            [--tile-markers] [--xp-drops] [--npc-labels] [--ground-items]""".trimIndent())
    }
}
