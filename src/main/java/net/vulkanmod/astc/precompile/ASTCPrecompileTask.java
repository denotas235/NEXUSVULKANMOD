package net.vulkanmod.astc.precompile;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Gradle task that pre-encodes PNG textures to ASTC during the build.
 *
 * Registered in build.gradle as:
 *   tasks.register('precompileAstc', ASTCPrecompileTask) {
 *       inputDir  = file('src/main/resources/assets')
 *       outputDir = file('build/astc-textures')
 *   }
 *   processResources.dependsOn precompileAstc
 *
 * Requires the {@code astcenc} CLI tool on PATH. Skips silently if not found.
 * Encoding is incremental: PNGs that have not changed since last run are skipped.
 */
@CacheableTask
public abstract class ASTCPrecompileTask extends DefaultTask {

    @InputDirectory
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getInputDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @TaskAction
    public void encode() throws IOException {
        // Check if astcenc CLI is available
        if (!isAstcencAvailable()) {
            getProject().getLogger().lifecycle("[ASTC] astcenc não encontrado no PATH — tarefa ignorada");
            return;
        }

        Path inputRoot  = getInputDir().get().getAsFile().toPath();
        Path outputRoot = getOutputDir().get().getAsFile().toPath();
        Files.createDirectories(outputRoot);

        int[] counts = {0, 0}; // [encoded, skipped]

        Files.walkFileTree(inputRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (!name.toLowerCase().endsWith(".png")) return FileVisitResult.CONTINUE;

                Path relative  = inputRoot.relativize(file);
                Path outputFile = outputRoot.resolve(relative.toString().replace(".png", ".astc"));
                Files.createDirectories(outputFile.getParent());

                // Incremental: skip if output is newer than input
                if (Files.exists(outputFile) &&
                        Files.getLastModifiedTime(outputFile).compareTo(Files.getLastModifiedTime(file)) > 0) {
                    counts[1]++;
                    return FileVisitResult.CONTINUE;
                }

                // Select block size from path heuristic
                String blockSize = selectBlockSize(relative.toString());

                int result = new ProcessBuilder(
                        "astcenc", "-cl", file.toString(), outputFile.toString(), blockSize, "thorough")
                        .redirectErrorStream(true)
                        .start()
                        .waitFor();

                if (result == 0) {
                    counts[0]++;
                } else {
                    getProject().getLogger().warn("[ASTC] Falha ao codificar: {}", file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        getProject().getLogger().lifecycle("[ASTC] Pré-compilação: {} codificados, {} ignorados",
                counts[0], counts[1]);
    }

    private static String selectBlockSize(String path) {
        String lc = path.toLowerCase();
        if (lc.contains("gui") || lc.contains("font") || lc.contains("item")) return "4x4";
        if (lc.contains("environment") || lc.contains("sky")) return "8x8";
        return "6x6";
    }

    private static boolean isAstcencAvailable() {
        try {
            return new ProcessBuilder("astcenc", "--help")
                    .start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
