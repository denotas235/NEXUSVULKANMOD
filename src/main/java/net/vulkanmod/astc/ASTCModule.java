package net.vulkanmod.astc;

import net.vulkanmod.Initializer;
import org.apache.logging.log4j.Logger;

/**
 * Entry point for the ASTC texture compression module.
 *
 * Initialised from {@link net.vulkanmod.Initializer#onInitializeClient()}.
 * The module checks hardware capabilities at startup and enables or
 * disables the ASTC pipeline accordingly. If ASTC is not supported,
 * the game runs normally without any compression.
 */
public class ASTCModule {
    private static final Logger LOGGER = Initializer.LOGGER;

    // Config (can be wired to the VulkanMod config system later)
    public static boolean astcEnabled = true;
    public static ASTCEncoder.Quality astcQuality = ASTCEncoder.Quality.FAST;
    public static long astcCacheSizeMb = 512L;

    private static boolean active = false;

    /**
     * Call once during client init, after Vulkan device selection.
     */
    public static void initialize() {
        if (!astcEnabled) {
            LOGGER.info("[ASTC] Module disabled via config");
            return;
        }

        try {
            ASTCCapabilities.init();

            if (!ASTCCapabilities.isAnyAstcSupported()) {
                LOGGER.info("[ASTC] Dispositivo não suporta ASTC — module inactivo");
                return;
            }

            ASTCTextureCache.init(astcCacheSizeMb);

            active = true;
            LOGGER.info("[ASTC] Module activo — qualidade: {}, cache: {}MB",
                    astcQuality.name(), astcCacheSizeMb);

        } catch (Exception e) {
            LOGGER.error("[ASTC] Falha na inicialização — module desactivado: {}", e.getMessage());
            active = false;
        }
    }

    /**
     * Returns true if ASTC is available and enabled on this device.
     */
    public static boolean isActive() {
        return active;
    }

    public static void shutdown() {
        if (active) {
            ASTCEncoder.shutdown();
            active = false;
        }
    }
}
