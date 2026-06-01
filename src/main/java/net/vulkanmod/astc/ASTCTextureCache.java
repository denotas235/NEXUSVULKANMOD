package net.vulkanmod.astc;

import net.fabricmc.loader.api.FabricLoader;
import net.vulkanmod.Initializer;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * Disk cache for pre-encoded ASTC textures.
 *
 * Cache layout:
 *   .minecraft/astc-cache/<sha256_of_original>_<vkFormat>.astc
 *
 * Entries older than {@link #MAX_CACHE_AGE_DAYS} days are automatically
 * evicted. Maximum total cache size is configurable (default 512 MB).
 */
public class ASTCTextureCache {
    private static final Logger LOGGER = Initializer.LOGGER;

    private static final int MAX_CACHE_AGE_DAYS = 30;
    private static final long DEFAULT_MAX_CACHE_MB = 512L;

    private static Path cacheDir;
    private static long maxCacheBytes;

    public static void init(long maxCacheMb) {
        maxCacheBytes = maxCacheMb * 1024 * 1024;
        cacheDir = FabricLoader.getInstance()
                .getGameDir()
                .resolve("astc-cache");
        try {
            Files.createDirectories(cacheDir);
            LOGGER.info("[ASTC] Cache dir: {} (max {}MB)", cacheDir, maxCacheMb);
            evictOldEntries();
        } catch (IOException e) {
            LOGGER.error("[ASTC] Não foi possível criar cache dir: {}", e.getMessage());
            cacheDir = null;
        }
    }

    public static void init() {
        init(DEFAULT_MAX_CACHE_MB);
    }

    /** Look up a cached ASTC texture. Returns null on miss. */
    public static ByteBuffer get(byte[] originalBytes, int vkFormat) {
        if (cacheDir == null) return null;
        Path entry = cacheEntry(originalBytes, vkFormat);
        if (!Files.exists(entry)) return null;
        try {
            byte[] raw = Files.readAllBytes(entry);
            ByteBuffer buf = ByteBuffer.allocateDirect(raw.length);
            buf.put(raw).flip();
            return buf;
        } catch (IOException e) {
            return null;
        }
    }

    /** Store an encoded ASTC texture in the cache. */
    public static void put(byte[] originalBytes, int vkFormat, ByteBuffer astcData) {
        if (cacheDir == null || astcData == null) return;
        Path entry = cacheEntry(originalBytes, vkFormat);
        try {
            byte[] raw = new byte[astcData.remaining()];
            astcData.duplicate().get(raw);
            Files.write(entry, raw, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            LOGGER.debug("[ASTC] Falha ao escrever cache: {}", e.getMessage());
        }
    }

    private static Path cacheEntry(byte[] originalBytes, int vkFormat) {
        String hash = sha256Hex(originalBytes);
        return cacheDir.resolve(hash + "_" + vkFormat + ".astc");
    }

    private static String sha256Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(java.util.Arrays.hashCode(data));
        }
    }

    private static void evictOldEntries() {
        if (cacheDir == null) return;
        Instant cutoff = Instant.now().minus(MAX_CACHE_AGE_DAYS, ChronoUnit.DAYS);
        try {
            Files.walkFileTree(cacheDir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.lastModifiedTime().toInstant().isBefore(cutoff)) {
                        Files.deleteIfExists(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.debug("[ASTC] Eviction error: {}", e.getMessage());
        }
    }
}
