package net.vulkanmod.mixin.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.ScissorState;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.vulkanmod.render.engine.*;
import net.vulkanmod.vulkan.Renderer;
import net.vulkanmod.vulkan.VRenderSystem;
import net.vulkanmod.vulkan.texture.VTextureSelector;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.OptionalDouble;
import java.util.OptionalInt;

@Mixin(RenderType.class)
public abstract class CompositeRenderTypeM {

    @Shadow @Final private RenderSetup state;

    @Shadow public abstract RenderPipeline pipeline();

    // TODO
    /**
     * @author
     * @reason
     */
    @Overwrite
    public void draw(MeshData meshData) {
        GpuBufferSlice gpuBufferSlice = RenderSystem.getDynamicUniforms()
                                                    .writeTransform(
                                                            RenderSystem.getModelViewMatrix(),
                                                            new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                                                            new Vector3f()
                                                    );
        MeshData var3 = meshData;

        try {
            GpuBuffer gpuBuffer = this.pipeline().getVertexFormat().uploadImmediateVertexBuffer(meshData.vertexBuffer());
            GpuBuffer gpuBuffer2;
            VertexFormat.IndexType indexType;
            if (meshData.indexBuffer() == null) {
                RenderSystem.AutoStorageIndexBuffer autoStorageIndexBuffer = RenderSystem.getSequentialBuffer(meshData.drawState().mode());
                gpuBuffer2 = autoStorageIndexBuffer.getBuffer(meshData.drawState().indexCount());
                indexType = autoStorageIndexBuffer.type();
            } else {
                gpuBuffer2 = this.pipeline().getVertexFormat().uploadImmediateIndexBuffer(meshData.indexBuffer());
                indexType = meshData.drawState().indexType();
            }

            RenderTarget renderTarget = ((CompositeStateAccessor)(Object)this.state).getOutputTarget().getRenderTarget();
            GpuTextureView gpuTextureView = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : renderTarget.getColorTextureView();
            GpuTextureView gpuTextureView2 = renderTarget.useDepth
                    ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : renderTarget.getDepthTextureView())
                    : null;

            try (RenderPass renderPass = RenderSystem.getDevice()
                                                     .createCommandEncoder()
                                                     .createRenderPass(() -> "Immediate draw for " +
                                                                             ((RenderType) (Object) (this)).toString(),
                                                                       gpuTextureView, OptionalInt.empty(), gpuTextureView2, OptionalDouble.empty())) {
                renderPass.setPipeline(this.pipeline());
                ScissorState scissorState = RenderSystem.getScissorStateForRenderTypeDraws();
                if (scissorState.enabled()) {
                    renderPass.enableScissor(scissorState.x(), scissorState.y(), scissorState.width(), scissorState.height());
                }

                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice);
                renderPass.setVertexBuffer(0, gpuBuffer);

                VRenderSystem.applyModelViewMatrix(RenderSystem.getModelViewMatrix());
                VRenderSystem.calculateMVP();

                renderPass.setIndexBuffer(gpuBuffer2, indexType);

                VkCommandEncoder commandEncoder = (VkCommandEncoder) RenderSystem.getDevice().createCommandEncoder();
                commandEncoder.trySetup((VkRenderPass) renderPass);

                Renderer.getDrawer().draw(meshData.vertexBuffer(), meshData.indexBuffer(), meshData.drawState().mode(), meshData.drawState().format(), meshData.drawState().vertexCount());
            }
        } catch (Throwable var17) {
            if (meshData != null) {
                try {
                    var3.close();
                } catch (Throwable var14) {
                    var17.addSuppressed(var14);
                }
            }

            throw var17;
        }

        if (meshData != null) {
            meshData.close();
        }
    }

}
