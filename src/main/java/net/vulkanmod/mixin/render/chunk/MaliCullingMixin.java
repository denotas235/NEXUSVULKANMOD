package net.vulkanmod.mixin.render.chunk;

import net.minecraft.client.Camera;
import net.minecraft.client.renderer.culling.Frustum;
import net.vulkanmod.render.chunk.WorldRenderer;
import net.vulkanmod.vulkan.device.DeviceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mali G57 (ARM, vendorId 0x13B5) culling optimisation.
 *
 * Mali is a TBDR (Tile-Based Deferred Renderer) GPU.
 * Reducing the geometry sent from CPU to GPU (bandwidth) is the primary
 * lever for performance. Forcing a fresh SectionGraph update every frame
 * ensures the frustum-culled section set is always exact, minimising
 * the number of draw calls issued to the GPU.
 *
 * remap = false: WorldRenderer is VulkanMod's own class; no obfuscation
 * remapping is required for method or field lookups.
 */
@Mixin(value = WorldRenderer.class, remap = false)
public abstract class MaliCullingMixin {

    @Shadow private boolean graphNeedsUpdate;

    /**
     * Method confirmed in WorldRenderer.java line 155:
     *   public void setupRenderer(Camera camera, Frustum frustum,
     *                             boolean isCapturedFrustum, boolean spectator)
     *
     * The inject runs at HEAD, before any camera-movement checks.
     * If the device is a Mali GPU and the frustum is not captured (debug mode),
     * we mark the graph dirty so SectionGraph.update() is guaranteed to run
     * this frame, giving the GPU an up-to-date culled section list.
     *
     * Safe fallback: on non-Mali GPUs the body is never reached (early return).
     * No cancellation — normal flow continues after this hook.
     */
    @Inject(method = "setupRenderer", at = @At("HEAD"))
    private void maliForceGraphUpdate(
            Camera camera,
            Frustum frustum,
            boolean isCapturedFrustum,
            boolean spectator,
            CallbackInfo ci) {

        if (isCapturedFrustum) {
            return;
        }

        if (DeviceManager.device == null || !DeviceManager.device.isMali()) {
            return;
        }

        this.graphNeedsUpdate = true;
    }
}
