import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
    alias(libs.plugins.shadow)
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

group = "com.github.wickidcow"
version = "3.1.7-Legacy3"

val archiveName = "SF_RykenSlimeCustomizer"
val slimefunLegacyVersion = "4.1.41"

java {
    // Paper 26.2 publishes Java 25 API classes. Build with Java 25 while
    // preserving Java 21 bytecode compatibility for the addon itself.
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    (options as StandardJavadocDocletOptions).apply {
        encoding = "UTF-8"
        charSet = "UTF-8"
        addStringOption("Xdoclint", "none")
    }
}

tasks.withType<JavaExec>().configureEach {
    systemProperty("file.encoding", "UTF-8")
    systemProperty("sun.stdout.encoding", "UTF-8")
    systemProperty("sun.stderr.encoding", "UTF-8")
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.alessiodp.com/releases/")
    maven("https://jitpack.io")
    maven("https://repo.minebench.de/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
    maven("https://mvn.lumine.io/repository/maven-public/")

    // Compile against the exact Slimefun Legacy production release instead of
    // a moving Slimefun/Gugu dependency. The release JAR is intentionally used
    // as an artifact-only Ivy repository so no transitive metadata is required.
    ivy {
        name = "slimefunLegacyRelease"
        url = uri("https://github.com/wickidcow/Slimefun-Legacy/releases/download/v$slimefunLegacyVersion")
        patternLayout {
            artifact("[artifact][revision].[ext]")
        }
        metadataSources {
            artifact()
        }
        content {
            includeModule("com.github.wickidcow", "Slimefun-Legacy")
        }
    }
}

dependencies {
    implementation(libs.libby.bukkit)
    implementation(libs.uni.item.all) {
        exclude(group = "io.github.projectunified", module = "uni-item-slimefun")
    }

    compileOnly(libs.graalvm.js)
    compileOnly(libs.graalvm.js.language)
    compileOnly(libs.graalvm.js.scriptengine)
    compileOnly(libs.graalvm.shadowed.icu4j)
    compileOnly(libs.graalvm.truffle.api)
    compileOnly(libs.graalvm.truffle.compiler)
    compileOnly(libs.graalvm.truffle.enterprise)
    compileOnly(libs.graalvm.truffle.runtime)
    compileOnly(libs.graalvm.polyglot)
    compileOnly(libs.graalvm.sdk.collections)
    compileOnly(libs.graalvm.sdk.nativeimage)
    compileOnly(libs.graalvm.sdk.word)
    compileOnly(libs.graalvm.sdk.nativebridge)
    compileOnly(libs.graalvm.sdk.jniutils)
    compileOnly(libs.graalvm.regex)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.byte.buddy)
    compileOnly(libs.paper.api) {
        // Match Slimefun Legacy's build setup: Paper 26.2 is Java 25 even
        // though this addon deliberately emits Java 21 class files.
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
    compileOnly(libs.slimefun.legacy)
    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)
    compileOnly(libs.item.nbt.api.plugin)
    compileOnly(libs.justenoughguide)
    compileOnly(libs.logitech)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    enabled = false
}

tasks.named<ProcessResources>("processResources") {
    filesMatching("**/*.yml") {
        expand(mapOf("version" to project.version))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    // Slimefun Legacy addon convention: raw SF_<Addon><Version>.jar release asset.
    archiveFileName.set("${archiveName}${project.version}.jar")
    relocate("io.github.projectunified.uniitem", "org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.uniitem")
    relocate("net.byteflux.libby", "org.lins.mmmjjkx.rykenslimefuncustomizer.libraries.libby")
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.runServer {
    dependsOn(tasks.named("shadowJar"))
    val run = file(providers.gradleProperty("server.run.dir").orElse("run"))
    runDirectory.set(run)

    doFirst {
        run.resolve("eula.txt").writeText("eula=true")
        val pluginsDir = run.resolve("plugins")
        pluginsDir.mkdirs()
        copy {
            from(projectDir.resolve("build/libs")) {
                include("${archiveName}${project.version}.jar")
            }
            into(pluginsDir)
        }
    }

    jvmArgs(
        "-Dfile.encoding=UTF-8",
        "-Dsun.jnu.encoding=UTF-8",
        "-Dnet.kyori.adventure.text.warn_when_legacy_formatting_detected=false"
    )
    maxHeapSize = "4G"
    minecraftVersion("1.21.11")
}
