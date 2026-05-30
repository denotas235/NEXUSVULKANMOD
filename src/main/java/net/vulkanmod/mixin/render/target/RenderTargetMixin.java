package net.vulkanmod.mixin.render.target;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.renderer.RenderPipelines;
import net.vulkanmod.render.engine.VkFbo;
import net.vulkanmod.render.engine.VkGpuTexture;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.OptionalInt;

@Mixin(RenderTarget.class)
public abstract class RenderTargetMixin {

    @Shadow public int width;
    @Shadow public int height;

    @Shadow @Nullable protected GpuTexture colorTexture;
    @Shadow @Nullable protected GpuTexture depthTexture;
    @Shadow @Nullable protected GpuTextureView colorTextureView;

    @Overwrite
    public void blitAndBlendToTexture(GpuTextureView gpuTextureView) {
        // stub: RenderPass.bindSampler API changed in MC 1.21.11
    }

//    @Inject(method = "getColorTextureView", at = @At("HEAD"))
//    private void injClear(CallbackInfoReturnable<GpuTextureView> cir) {
//        applyClear();
//    }
//
//    @Unique
//    private void applyClear() {
//        VkFbo fbo = ((VkGpuTexture) this.colorTexture).getFbo(this.depthTexture);
//        if (fbo.needsClear()) {
//            fbo.bind();
//        }
//    }
}
