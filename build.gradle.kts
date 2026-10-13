plugins {
    kotlin("multiplatform") version "2.2.20"
}

description = "The RuneScape client from December 2010 (build 634), with a launcher for any Void server."

/*
 * A Kotlin Multiplatform project with one target, the JVM.
 * The game is in src/commonMain, on shims of the Java APIs it uses; the native classes keep their
 * JNI names and signatures in src/jvmMain, with expect declarations in commonMain.
 */
kotlin {
    jvmToolchain(21)
    jvm()
    /*
     * The browser target: ./gradlew jsBrowserDevelopmentWebpack
     * builds a servable bundle in build/kotlin-webpack/js/developmentExecutable. It also keeps
     * commonMain honest: compileCommonMainKotlinMetadata compiles it against the common standard
     * library, not the JDK.
     */
    js {
        browser {
            commonWebpackConfig {
                outputFileName = "runetek5-634.js"
            }
        }
        binaries.executable()
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets {
        jvmMain.dependencies {
            implementation(files("lib/stub.jar"))
        }
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
    }
}

val jvmJar = tasks.named<Jar>("jvmJar")
val jvmRuntimeClasspath = configurations.named("jvmRuntimeClasspath")
val clientClasspath = files(jvmJar, jvmRuntimeClasspath)
val clientLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }

/* The JVM options of the launcher, the client and the start scripts. */
val clientJvmArgs = listOf(
    "-Xmx256m",
    "-Dsun.java2d.noddraw=true",
    "--add-opens",
    "java.base/java.lang=ALL-UNNAMED",
    "--enable-native-access=ALL-UNNAMED",
)

/*
 * The directory the client unpacks its native libraries into goes on LD_LIBRARY_PATH:
 * -Pclient.nativeDir=<dir>, or on Linux by default ~/.jagex_cache_32/runescape, where the live game0
 * client unpacks them. libhw3d.so (behind the native OpenGL toolkit) links against libjaclib.so by
 * name, and libjaclib.so has no SONAME, so without this the system's loader cannot find it and the
 * client falls back to the Java software toolkit, which looks like safe mode. Not needed for the Java
 * toolkits.
 */
val nativeDir = providers.gradleProperty("client.nativeDir").orNull
    ?: if (System.getProperty("os.name").startsWith("Linux")) System.getProperty("user.home") + "/.jagex_cache_32/runescape" else null

fun JavaExec.withNativeDir() {
    nativeDir?.let { dir ->
        val existing = System.getenv("LD_LIBRARY_PATH")
        environment("LD_LIBRARY_PATH", if (existing.isNullOrEmpty()) dir else "$dir:$existing")
    }
}

/* ./gradlew run starts the launcher: ./gradlew run --args="--address play.example.org --zoom". */
tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the launcher, RuneTek5, with --args."
    classpath = clientClasspath
    mainClass = "RuneTek5"
    javaLauncher = clientLauncher
    jvmArgs = clientJvmArgs
    withNativeDir()
}

/*
 * ./gradlew installDist: the client with start scripts in build/install/runetek5-634
 * (bin/runetek5-634, or bin\runetek5-634.bat on Windows), which start the launcher.
 */
val startScripts = tasks.register<CreateStartScripts>("startScripts") {
    applicationName = "runetek5-634"
    mainClass = "RuneTek5"
    defaultJvmOpts = clientJvmArgs
    classpath = clientClasspath
    outputDir = layout.buildDirectory.dir("scripts").get().asFile
}

tasks.register<Sync>("installDist") {
    group = "distribution"
    description = "Installs the client with start scripts into build/install/runetek5-634."
    into(layout.buildDirectory.dir("install/runetek5-634"))
    into("lib") {
        from(clientClasspath)
    }
    into("bin") {
        from(startScripts)
    }
}

/*
 * ./gradlew runClient starts the client itself against a local Void server.
 * The client decides its own ports in local mode: the game port is the world id plus 40000, so world
 * 3594 connects to 43594. Switches for behaviour that is not part of the original client (see
 * ClientFeatures) are passed as -Pclient.debug=true and the like; none is set unless asked for.
 */
tasks.register<JavaExec>("runClient") {
    group = "application"
    description = "Runs the client itself against a local Void server, with -Pclient.* switches."
    classpath = clientClasspath
    mainClass = "client"
    javaLauncher = clientLauncher
    jvmArgs = clientJvmArgs
    args = listOf("3594", "3594", "0", "live", "english", "game0")
    listOf("client.debug", "client.zoom", "client.zoomKey", "client.renderDistance", "client.console", "client.shiftDrop", "client.msaa", "client.smaa", "client.renderScale", "client.a2c", "client.server", "client.packets", "client.fps", "client.scale", "client.address", "client.port",
        "client.test", "client.test.user", "client.test.password", "client.test.frames", "client.test.timeout",
        "client.test.shot", "client.test.reference", "client.test.tolerance", "client.test.detailTolerance", "client.test.command", "client.test.commandFrames", "client.test.chat", "client.test.chatFrames", "client.test.windowModes", "client.test.subInterfaces", "client.test.menuWalk", "client.test.menuWalkHome", "client.test.media", "client.test.markers", "client.tileMarkers", "client.xpDrops", "client.npcLabels", "client.groundItemLabels", "client.test.groundItems", "client.test.npcLabels", "client.test.xpDrops", "client.test.toolkit", "client.test.bench", "client.test.benchMode")
        .forEach { name -> providers.gradleProperty(name).orNull?.let { systemProperty(name, it) } }
    withNativeDir()
}

/*
 * The release jar: everything the client needs in one runnable jar. The manifest opens java.lang the
 * way --add-opens does on the command line, so `java -jar` and a double click both work.
 */
val releaseJar = tasks.register<Jar>("releaseJar") {
    group = "distribution"
    description = "Builds the runnable release jar that starts RuneTek5."
    archiveBaseName = "runetek5-634"
    archiveVersion = project.version.toString().takeUnless { it == "unspecified" } ?: "dev-build"
    destinationDirectory = layout.buildDirectory.dir("release")
    manifest {
        attributes(
            "Main-Class" to "RuneTek5",
            "Add-Opens" to "java.base/java.lang",
            "Enable-Native-Access" to "ALL-UNNAMED",
            "Implementation-Version" to archiveVersion.get(),
        )
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(jvmJar.map { zipTree(it.archiveFile) })
    from(jvmRuntimeClasspath.map { classpath -> classpath.map { zipTree(it) } }) {
        exclude("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }
}

/*
 * The browser client for a release: the production bundle with its page and the SMAA files, zipped
 * beside the jar. Served behind a WebSocket relay to a game server (README: "Running the browser client").
 */
val releaseBrowser = tasks.register<Zip>("releaseBrowser") {
    group = "distribution"
    description = "Zips the browser bundle beside the release jar."
    archiveBaseName = "runetek5-634-browser"
    archiveVersion = project.version.toString().takeUnless { it == "unspecified" } ?: "dev-build"
    destinationDirectory = layout.buildDirectory.dir("release")
    from(tasks.named("jsBrowserDistribution"))
}

tasks.register("release") {
    group = "distribution"
    description = "Builds the release jar and the browser zip."
    dependsOn(releaseJar, releaseBrowser)
}

/*
 * ./gradlew checkClasses (part of check, run by CI): checks that need no game server. Every class of
 * the client must load and pass the JVM's verifier from the release jar, and no method may be over
 * HotSpot's 8000-byte JIT limit unless config/huge-methods.txt lists it (see src/ci/java/CheckClasses).
 * -PcheckClasses.java=25 runs it on another JDK than the toolchain's.
 */
val compileCi = tasks.register<JavaCompile>("compileCi") {
    source = fileTree("src/ci/java")
    classpath = files()
    destinationDirectory = layout.buildDirectory.dir("classes/java/ci")
    javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(21) }
    options.encoding = "UTF-8"
}

tasks.register<JavaExec>("checkClasses") {
    group = "verification"
    description = "Loads and verifies every client class from the release jar, and checks method sizes."
    val clientJar = jvmJar.flatMap { it.archiveFile }
    val allowlist = layout.projectDirectory.file("config/huge-methods.txt")
    inputs.file(clientJar)
    inputs.file(allowlist)
    classpath = files(compileCi) + files(releaseJar.flatMap { it.archiveFile })
    mainClass = "CheckClasses"
    jvmArgs("-Djava.awt.headless=true")
    argumentProviders.add(CommandLineArgumentProvider { listOf(clientJar.get().asFile.path, allowlist.asFile.path) })
    javaLauncher = javaToolchains.launcherFor {
        languageVersion = JavaLanguageVersion.of(providers.gradleProperty("checkClasses.java").getOrElse("21").toInt())
    }
}

tasks.check {
    dependsOn("checkClasses")
}

/*
 * The SMAA shader and lookup textures (src/jvmMain/resources/smaa) go beside the browser bundle too:
 * with client.smaa the page fetches them before the client starts (main.js.kt).
 */
tasks.named<ProcessResources>("jsProcessResources") {
    from("src/jvmMain/resources") {
        include("smaa/**")
    }
}

/*
 * The development bundle's folder is servable as it is: the page (index.html) and its icon are
 * copied in beside runetek5-634.js. Serve it with node tools/serve.mjs, with the relay running
 * (node tools/relay.mjs), as the README's "Running the browser client" says.
 */
val jsCopyDevResources = tasks.register<Copy>("jsCopyDevResources") {
    from(tasks.named("jsProcessResources"))
    into(layout.buildDirectory.dir("kotlin-webpack/js/developmentExecutable"))
}

tasks.named("jsBrowserDevelopmentWebpack") {
    finalizedBy(jsCopyDevResources)
}
