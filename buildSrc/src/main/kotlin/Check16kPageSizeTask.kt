import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ProjectLayout
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

abstract class Check16kPageSizeTask : DefaultTask() {

    @get:Internal
    abstract val sdkDir: DirectoryProperty

    @get:javax.inject.Inject
    abstract val layout: ProjectLayout

    @TaskAction
    fun verify() {
        val sdk = sdkDir.get().asFile
        val readelf = locateLlvmReadelf(sdk) ?: throw GradleException(
            "llvm-readelf not found under ${sdk}/ndk. Install an NDK."
        )
        val outputsDir = layout.buildDirectory.dir("outputs").get().asFile
        val artifacts = outputsDir.walkTopDown()
            .filter { it.isFile && it.parentFile.name == "release" }
            .filter { it.name.endsWith(".aab") || it.name.endsWith(".apk") }
            .sortedBy { it.absolutePath }
            .toList()

        if (artifacts.isEmpty()) {
            throw GradleException(
                "No release APK/AAB found under $outputsDir. " +
                    "Run :androidApp:bundleRelease or :androidApp:assembleRelease first."
            )
        }

        val allowedAbis = setOf("arm64-v8a", "x86_64")
        val errors = mutableListOf<String>()
        var libCount = 0
        val tmp = layout.buildDirectory.dir("tmp/check16kPageSize").get().asFile.apply {
            deleteRecursively()
            mkdirs()
        }

        artifacts.forEach { artifact ->
            ZipFile(artifact).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    if (entry.isDirectory || !entry.name.endsWith(".so")) return@forEach
                    val name = entry.name
                    val abi = allowedAbis.firstOrNull {
                        name.startsWith("lib/$it/") || name.contains("/lib/$it/")
                    } ?: return@forEach
                    libCount++
                    val so = tmp.resolve(name.replace('/', '_'))
                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(so).use { output -> input.copyTo(output) }
                    }
                    val pb = ProcessBuilder(readelf.absolutePath, "-lW", so.absolutePath)
                        .redirectErrorStream(true)
                    val proc = pb.start()
                    val exitCode = proc.waitFor()
                    val output = proc.inputStream.bufferedReader().readText()
                    if (exitCode != 0) {
                        errors += "[readelf failed] artifact=${artifact.name} abi=$abi file=$name"
                        return@forEach
                    }
                    output.lineSequence().forEach { line ->
                        if (line.trimStart().startsWith("LOAD")) {
                            val align = line.trim().split(Regex("\\s+")).last()
                            val value = align.removePrefix("0x").toLongOrNull(16) ?: return@forEach
                            if (value < 0x4000L || value % 0x4000L != 0L) {
                                val msg = "[bad align=$align] artifact=${artifact.name} abi=$abi file=$name"
                                if (msg !in errors) errors += msg
                            }
                        }
                    }
                }
            }
        }

        if (errors.isNotEmpty()) {
            throw GradleException("16 KB page-size check failed:\n" + errors.joinToString("\n"))
        }
        logger.lifecycle("check16kPageSize: OK ($libCount native lib(s) in ${artifacts.size} artifact(s) scanned)")
    }

    private fun locateLlvmReadelf(sdkDir: File): File? {
        val ndkRoot = sdkDir.resolve("ndk")
        val dirs = ndkRoot.listFiles()?.filter { it.isDirectory }
            ?.sortedWith(compareByDescending { it.name }) ?: return null
        for (dir in dirs) {
            val prebuilt = dir.resolve("toolchains/llvm/prebuilt").listFiles()?.filter { it.isDirectory } ?: continue
            for (host in prebuilt.sortedByDescending { it.name }) {
                val readelf = host.resolve("bin/llvm-readelf")
                if (readelf.isFile) return readelf
            }
        }
        return null
    }
}
