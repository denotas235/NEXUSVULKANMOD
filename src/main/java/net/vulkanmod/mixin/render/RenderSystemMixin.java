package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.shaders.ShaderSource;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.DynamicUniforms;
import net.vulkanmod.render.engine.VkGpuDevice;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

    @Shadow private static Thread renderThread;
    @Shadow private static GpuDevice DEVICE;
    @Shadow private static String apiDescription;
    @Shadow private static DynamicUniforms dynamicUniforms;

    @Shadow
    public static void assertOnRenderThread() {}

    /**
     * @author VulkanMod fork
     * @reason Replace OpenGL renderer with Vulkan
     */
    @Overwrite
    public static void initRenderer(long window, int debugVerbosity, boolean bl,
            ShaderSource shaderSource, boolean bl2) {
        renderThread.setPriority(Thread.NORM_PRIORITY + 2);

        VRenderSystem.initRenderer();

        DEVICE = new VkGpuDevice(window, debugVerbosity, bl, shaderSource, bl2);
        apiDescription = RenderSystem.getDevice().getImplementationInformation();

        Renderer.initRenderer();

        dynamicUniforms = new DynamicUniforms();
    }
}
