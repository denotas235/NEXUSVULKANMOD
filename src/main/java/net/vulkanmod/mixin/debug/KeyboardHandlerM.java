package net.vulkanmod.mixin.debug;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerM {

    @Shadow(remap = false)
    private boolean field_1679;

    @Shadow(remap = false)
    protected abstract boolean method_1468(KeyEvent keyEvent);

    @Inject(
        method = "method_1466",
        remap = false,
        at = @At("HEAD")
    )
    private void chunkDebug(long l, int i, KeyEvent keyEvent, CallbackInfo ci) {
        if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), 296)) {
            this.field_1679 |= this.method_1468(keyEvent);
        }
    }
}
