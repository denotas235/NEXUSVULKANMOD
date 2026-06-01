package net.vulkanmod.astc.precompile;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Standalone ASTC pre-compilation utility (no Gradle API dependency).
 *
 * Can be invoked from the command line to pre-encode PNG assets:
 *   java -cp VulkanMod.jar net.vulkanmod.astc.precompile.ASTCPrecompileTask \
 *        <inputDir> <outputDir> [maxAgeDays]
 *
 * Or wired up in build.gradle via a JavaExec task (no buildSrc needed):
 *
 *   tasks.register('precompileAstc', JavaExec) {
 *       classpath = sourceSets.main.runtimeClasspath
 *       mainClass = 'net.vulkanmod.astc.precompile.ASTCPrecompileTask'
 *       args = [
 *           file('src/main/resources/assets').absolutePath,
 *           file('build/astc-textures').absolutePath
 *       ]
 *   }
 *   processResources.dependsOn precompileAstc
 *
 * Requires the {@code astcenc} CLI tool to be on PATH.
 * Silently skips encoding if astcenc is not found.
 * Incremental: output files newer than their source are skipped.
 */
public class ASTCPrecompileTask {

    private final Path inputRoot;
    private final Path outputRoot;
    private final int maxAgeDays;

    public ASTCPrecompileTask(Path inputRoot, Path outputRoot, int maxAgeDays) {
        this.inputRoot  = inputRoot;
        this.outputRoot = outputRoot;
        this.maxAgeDays = maxAgeDays;
    }

    /** Entry point for CLI / JavaExec invocation. */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ASTCPrecompileTask <inputDir> <outputDir> [maxAgeDays]");
            System.exit(1);
        }
        Path input  = Path.of(args[0]);
        Path output = Path.of(args[1]);
        int days = args.length > 2 ? Integer.parseInt(args[2]) : 30;

        try {
            new ASTCPrecompileTask(input, output, days).run();
        } catch (Exception e) {
            System.err.println("[ASTC] Pre-compile failed: " + e.getMessage());
            System.exit(1);
        }
    }

    public void run() throws IOException {
        if (!isAstcencAvailable()) {
            System.out.println("[ASTC] astcenc not found on PATH — skipping pre-compile");
            return;
        }

        Files.createDirectories(outputRoot);
        int[] counts = {0, 0};   // [encoded, skipped]
        long[] bytes  = {0};

        Files.walkFileTree(inputRoot, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (!name.toLowerCase().endsWith(".png")) return FileVisitResult.CONTINUE;

                Path relative   = inputRoot.relativize(file);
                Path outputFile = outputRoot.resolve(relative.toString().replace(".png", ".astc"));
                Files.createDirectories(outputFile.getParent());

                if (Files.exists(outputFile) &&
                        Files.getLastModifiedTime(outputFile).compareTo(
                                Files.getLastModifiedTime(file)) > 0) {
                    counts[1]++;
                    return FileVisitResult.CONTINUE;
                }

                String block = selectBlockSize(relative.toString());
                try {
                    int result = new ProcessBuilder(
                            "astcenc", "-cl",
                            file.toString(), outputFile.toString(),
                            block, "thorough")
                            .redirectErrorStream(true)
                            .start()
                            .waitFor();

                    if (result == 0) {
                        counts[0]++;
                        bytes[0] += Files.size(outputFile);
                    } else {
                        System.err.printf("[ASTC] Failed to encode: %s%n", file);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return FileVisitResult.TERMINATE;
                }
                return FileVisitResult.CONTINUE;
            }
        });

        System.out.printf("[ASTC] Pre-compile done: %d encoded (%.1f KB), %d skipped%n",
                counts[0], bytes[0] / 1024.0, counts[1]);
    }

    private static String selectBlockSize(String path) {
        String lc = path.toLowerCase();
        if (lc.contains("gui") || lc.contains("font") || lc.contains("item")) return "4x4";
        if (lc.contains("environment") || lc.contains("sky"))                 return "8x8";
        return "6x6";
    }

    private static boolean isAstcencAvailable() {
        try {
            return new ProcessBuilder("astcenc", "--help")
                    .redirectErrorStream(true)
                    .start()
                    .waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
