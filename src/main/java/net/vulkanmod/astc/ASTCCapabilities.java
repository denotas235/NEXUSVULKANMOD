package net.vulkanmod.astc;

import net.vulkanmod.Initializer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class ASTCCapabilities {
    private static final Logger LOGGER = Initializer.LOGGER;

    private static final String EXT_ASTC_HDR = "VK_EXT_texture_compression_astc_hdr";
    private static final String EXT_ASTC_DECODE = "VK_EXT_astc_decode_mode";

    private static boolean astcLdrSupported = false;
    private static boolean astcHdrSupported = false;
    private static boolean astcDecodeModeSupported = false;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        initialized = true;

        if (DeviceManager.device == null || DeviceManager.physicalDevice == null) {
            LOGGER.warn("[ASTC] DeviceManager not initialized — ASTC disabled");
            return;
        }

        try {
            // Query ASTC LDR support directly from Vulkan physical device features
            astcLdrSupported = DeviceManager.device.availableFeatures.features().textureCompressionASTC_LDR();

            // Query extension support
            Set<String> unsupported = DeviceManager.device.getUnsupportedExtensions(
                    Set.of(EXT_ASTC_HDR, EXT_ASTC_DECODE));
            astcHdrSupported = !unsupported.contains(EXT_ASTC_HDR);
            astcDecodeModeSupported = !unsupported.contains(EXT_ASTC_DECODE);

            String deviceName = DeviceManager.device.deviceName;
            LOGGER.info("[ASTC] {}: LDR={}, HDR={}, DecodeMode={}",
                    deviceName, astcLdrSupported, astcHdrSupported, astcDecodeModeSupported);

            if (astcLdrSupported) {
                LOGGER.info("[ASTC] Formatos LDR disponíveis: ASTC 4x4 a 12x12 UNORM/SRGB");
            }
            if (astcHdrSupported) {
                LOGGER.info("[ASTC] Formatos HDR disponíveis: ASTC 4x4 a 12x12 SFLOAT");
            }
        } catch (Exception e) {
            LOGGER.error("[ASTC] Erro ao verificar capacidades: {}", e.getMessage());
            astcLdrSupported = false;
            astcHdrSupported = false;
            astcDecodeModeSupported = false;
        }
    }

    public static boolean isAstcLdrSupported() { return astcLdrSupported; }
    public static boolean isAstcHdrSupported() { return astcHdrSupported; }
    public static boolean isAstcDecodeModeSupported() { return astcDecodeModeSupported; }
    public static boolean isAnyAstcSupported() { return astcLdrSupported || astcHdrSupported; }
}
