package net.vulkanmod.mixin.render;

import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSetup.class)
public interface CompositeStateAccessor {

    @Accessor("outputTarget")
    OutputTarget getOutputTarget();
}
