package net.vulkanmod.astc.mixin;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.vulkanmod.astc.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

/**
 * Intercepts texture binding in AbstractTexture to optionally encode
 * the texture data as ASTC before uploading to the GPU.
 *
 * remap = false: required because refMap is not generated with Mojang mappings.
 * This mixin is only active when ASTCModule.isActive() returns true.
 */
@Mixin(value = AbstractTexture.class, remap = false)
public class TextureUploadMixin {

    /**
     * Hook into bind() — a stable method present across MC versions.
     * If ASTC is active and the encoder has compressed data available,
     * we could redirect the GPU upload here. Currently a no-op stub
     * that validates the pipeline is wired correctly.
     *
     * Full implementation requires access to the NativeImage pixel data
     * at the point of upload, which varies between MC versions.
     */
    @Inject(method = "bind()V", at = @At("HEAD"), remap = false)
    private void onBind(CallbackInfo ci) {
        // Guard — do nothing if module is inactive
        if (!ASTCModule.isActive()) return;
        // Actual ASTC upload logic requires NativeImage access at this point.
        // Handled in NativeImageMixin for the upload path.
    }
}
