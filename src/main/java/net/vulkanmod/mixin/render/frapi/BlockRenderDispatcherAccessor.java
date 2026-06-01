package net.vulkanmod.mixin.render.frapi;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BlockRenderDispatcher.class)
public interface BlockRenderDispatcherAccessor {

    @Accessor("modelRenderer")
    ModelBlockRenderer getModelRenderer();

    @Accessor("blockColors")
    BlockColors getBlockColors();
}
