package net.vulkanmod.astc;

import static org.lwjgl.vulkan.VK10.*;

/**
 * Selects the optimal ASTC block format based on texture type.
 * Format selection prioritises quality for small/UI textures and
 * performance for large terrain textures.
 */
public class ASTCFormatSelector {

    // ASTC LDR VkFormat constants (Vulkan 1.0 core — always available via VK10)
    public static final int ASTC_4x4_UNORM  = VK_FORMAT_ASTC_4x4_UNORM_BLOCK;
    public static final int ASTC_4x4_SRGB   = VK_FORMAT_ASTC_4x4_SRGB_BLOCK;
    public static final int ASTC_6x6_UNORM  = VK_FORMAT_ASTC_6x6_UNORM_BLOCK;
    public static final int ASTC_6x6_SRGB   = VK_FORMAT_ASTC_6x6_SRGB_BLOCK;
    public static final int ASTC_8x8_UNORM  = VK_FORMAT_ASTC_8x8_UNORM_BLOCK;
    public static final int ASTC_8x8_SRGB   = VK_FORMAT_ASTC_8x8_SRGB_BLOCK;

    // ASTC HDR SFLOAT — VK_FORMAT_ASTC_4X4_SFLOAT_BLOCK_EXT = 1000066000
    // Defined as literal to avoid dependency on EXTTextureCompressionAstcHdr
    // which may not be present in all bundled LWJGL versions.
    // Reference: https://registry.khronos.org/vulkan/specs/latest/man/html/VkFormat.html
    public static final int ASTC_4x4_SFLOAT = 1000066000; // VK_FORMAT_ASTC_4X4_SFLOAT_BLOCK_EXT

    /**
     * Select the best ASTC format for a texture identified by its resource path.
     *
     * @param texturePath the resource location string (e.g. "minecraft:textures/block/stone.png")
     * @param srgb        whether the texture is in sRGB colour space
     * @return VkFormat constant for the selected ASTC format
     */
    public static int selectFormat(String texturePath, boolean srgb) {
        if (texturePath == null) return srgb ? ASTC_6x6_SRGB : ASTC_6x6_UNORM;

        String lower = texturePath.toLowerCase();

        // Emissive / sky / HDR textures → ASTC 4×4 SFLOAT (HDR)
        if (ASTCCapabilities.isAstcHdrSupported()) {
            if (lower.contains("sky") || lower.contains("sun") || lower.contains("moon")
                    || lower.contains("emissive") || lower.contains("glow")) {
                return ASTC_4x4_SFLOAT;
            }
        }

        // UI / items / font / GUI — maximum quality
        if (lower.contains("gui") || lower.contains("font") || lower.contains("item")
                || lower.contains("hud") || lower.contains("icon") || lower.contains("inventory")) {
            return srgb ? ASTC_4x4_SRGB : ASTC_4x4_UNORM;
        }

        // Normal maps — high precision, linear
        if (lower.contains("normal") || lower.contains("_n.") || lower.contains("_normal")) {
            return ASTC_4x4_UNORM;
        }

        // Distant terrain / sky backgrounds — prefer performance
        if (lower.contains("environment") || lower.contains("terrain/far")
                || lower.contains("background")) {
            return srgb ? ASTC_8x8_SRGB : ASTC_8x8_UNORM;
        }

        // Regular blocks / entities — balanced quality
        return srgb ? ASTC_6x6_SRGB : ASTC_6x6_UNORM;
    }

    /**
     * Convenience overload — assumes sRGB for colour textures.
     */
    public static int selectFormat(String texturePath) {
        return selectFormat(texturePath, true);
    }
}
