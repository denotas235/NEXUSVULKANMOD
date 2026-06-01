package net.vulkanmod.astc;

import net.vulkanmod.Initializer;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ASTC encoder using libastcenc via JNI.
 *
 * If libastcenc is not available (e.g. not provided by the launcher),
 * all encode calls return {@code null} and the caller must fall back
 * to uncompressed upload. This class never crashes the JVM.
 *
 * Thread model: encoding is delegated to a single background thread
 * so the render thread is never blocked.
 */
public class ASTCEncoder {
    private static final Logger LOGGER = Initializer.LOGGER;

    /** Encoding quality presets (passed to libastcenc). */
    public enum Quality {
        FAST(10),       // Runtime encoding — speed priority
        THOROUGH(60);   // Pre-compile encoding — quality priority

        public final int level;
        Quality(int level) { this.level = level; }
    }

    private static final ExecutorService ENCODER_THREAD =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "ASTC-Encoder");
                t.setDaemon(true);
                return t;
            });

    private static boolean nativeAvailable = false;

    static {
        tryLoadNative();
    }

    private static void tryLoadNative() {
        // libastcenc is not bundled — the launcher must provide it
        // Paths where Zalith Launcher may expose native libraries
        String[] candidates = {
                "libastcenc.so",
                "/data/data/com.zalith.launcher/files/astcenc/libastcenc.so"
        };
        for (String path : candidates) {
            try {
                System.load(path);
                nativeAvailable = true;
                LOGGER.info("[ASTC] libastcenc carregado: {}", path);
                return;
            } catch (UnsatisfiedLinkError | SecurityException e) {
                // try next
            }
        }
        LOGGER.info("[ASTC] libastcenc não disponível — encoding ASTC desactivado (usar texturas originais)");
    }

    /**
     * Returns true if native ASTC encoding is available.
     */
    public static boolean isAvailable() {
        return nativeAvailable;
    }

    /**
     * Encodes raw RGBA pixel data to ASTC asynchronously.
     *
     * @param pixels  raw RGBA pixels (width * height * 4 bytes)
     * @param width   image width
     * @param height  image height
     * @param format  target VkFormat (ASTC variant)
     * @param quality encoding quality preset
     * @return Future resolving to encoded ASTC bytes, or null if unavailable
     */
    public static Future<ByteBuffer> encodeAsync(
            ByteBuffer pixels, int width, int height,
            int format, Quality quality) {

        if (!nativeAvailable || pixels == null) return null;

        return ENCODER_THREAD.submit(() -> encodeNative(pixels, width, height, format, quality.level));
    }

    /**
     * Synchronous encode — blocks caller thread.
     * Prefer {@link #encodeAsync} to avoid stalling the render thread.
     */
    public static ByteBuffer encode(
            ByteBuffer pixels, int width, int height,
            int format, Quality quality) {

        if (!nativeAvailable || pixels == null) return null;
        return encodeNative(pixels, width, height, format, quality.level);
    }

    // ---------------------------------------------------------------
    // JNI declarations — implemented in libastcenc native library
    // ---------------------------------------------------------------

    private static native ByteBuffer encodeNative(
            ByteBuffer pixels, int width, int height,
            int vkFormat, int qualityLevel);

    public static void shutdown() {
        ENCODER_THREAD.shutdownNow();
    }
}
