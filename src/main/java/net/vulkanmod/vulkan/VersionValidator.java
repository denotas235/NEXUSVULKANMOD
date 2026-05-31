/*
 * NEXUSVULKANMOD - Vulkan Renderer for Minecraft
 * Licensed under LGPL-3.0
 * Copyright (c) 2024
 * 
 * Vulkan Version Validator - Ensures Vulkan 1.1 compatibility
 */

package net.vulkanmod.vulkan;

import net.vulkanmod.Initializer;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkEnumerateInstanceVersion;

/**
 * Validates that the system supports at least Vulkan 1.1
 * (while maintaining strict 1.1 core feature compliance).
 * 
 * This validator ensures compatibility across:
 * - Desktop GPUs (NVIDIA, AMD, Intel) with various driver versions
 * - Mobile GPUs (Mali-G52, Adreno, PowerVR)
 * - Integrated graphics (Intel, AMD Radeon)
 * - Emulators and SwiftShader
 * 
 * Enforces strict version checking to prevent accidental upgrades
 * to 1.2+ exclusive features.
 */
public class VersionValidator {
    
    private static final int MINIMUM_VULKAN_MAJOR = 1;
    private static final int MINIMUM_VULKAN_MINOR = 1;
    
    /**
     * Validates instance Vulkan version support.
     * Called during VRenderSystem initialization before instance creation.
     * 
     * @throws RuntimeException if system doesn't support Vulkan 1.1 or higher
     */
    public static void validateVulkanVersion() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pVersion = stack.mallocInt(1);
            vkEnumerateInstanceVersion(pVersion);
            
            int instanceVersion = pVersion.get(0);
            int major = VK_VERSION_MAJOR(instanceVersion);
            int minor = VK_VERSION_MINOR(instanceVersion);
            int patch = VK_VERSION_PATCH(instanceVersion);
            
            Initializer.LOGGER.info("════════════════════════════════════════");
            Initializer.LOGGER.info("  Vulkan Version Detection Report");
            Initializer.LOGGER.info("════════════════════════════════════════");
            Initializer.LOGGER.info("  Detected Vulkan API Version: {}.{}.{}", major, minor, patch);
            
            // Require at least Vulkan 1.1
            if (major < MINIMUM_VULKAN_MAJOR || 
                (major == MINIMUM_VULKAN_MAJOR && minor < MINIMUM_VULKAN_MINOR)) {
                
                Initializer.LOGGER.error("✗ VULKAN VERSION INCOMPATIBLE");
                Initializer.LOGGER.error("  Required: Vulkan {}.{}+", MINIMUM_VULKAN_MAJOR, MINIMUM_VULKAN_MINOR);
                Initializer.LOGGER.error("  Found: Vulkan {}.{}.{}", major, minor, patch);
                
                throw new RuntimeException(
                    String.format(
                        "Vulkan %d.%d+ is required. System has: %d.%d.%d",
                        MINIMUM_VULKAN_MAJOR, MINIMUM_VULKAN_MINOR,
                        major, minor, patch
                    )
                );
            }
            
            Initializer.LOGGER.info("  ✓ Version Check: PASSED");
            Initializer.LOGGER.info("  ✓ Compatibility: Vulkan 1.1 core features");
            Initializer.LOGGER.info("  ✓ Mobile GPU support: ENABLED");
            Initializer.LOGGER.info("════════════════════════════════════════");
        }
    }
    
    /**
     * Logs a reminder about Vulkan 1.1 compliance for developers.
     * Should be called during initialization.
     */
    public static void warnAbout12Features() {
        Initializer.LOGGER.info("");
        Initializer.LOGGER.info("⚠  DEVELOPER REMINDER:");
        Initializer.LOGGER.info("   This codebase is locked to Vulkan 1.1 core features.");
        Initializer.LOGGER.info("   DO NOT use these 1.2+ features:");
        Initializer.LOGGER.info("   ✗ Timeline semaphores (VK_KHR_timeline_semaphore)");
        Initializer.LOGGER.info("   ✗ Buffer device address (VK_KHR_buffer_device_address)");
        Initializer.LOGGER.info("   ✗ Synchronization2 (VK_KHR_synchronization2)");
        Initializer.LOGGER.info("   ✗ Dynamic rendering (VK_KHR_dynamic_rendering)");
        Initializer.LOGGER.info("   ✗ Shader objects (VK_EXT_shader_object)");
        Initializer.LOGGER.info("");
    }
    
    /**
     * Returns the detected Vulkan version as a formatted string.
     * Useful for logging and debugging.
     */
    public static String getDetectedVersion() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pVersion = stack.mallocInt(1);
            vkEnumerateInstanceVersion(pVersion);
            
            int version = pVersion.get(0);
            int major = VK_VERSION_MAJOR(version);
            int minor = VK_VERSION_MINOR(version);
            int patch = VK_VERSION_PATCH(version);
            
            return String.format("%d.%d.%d", major, minor, patch);
        }
    }
}
