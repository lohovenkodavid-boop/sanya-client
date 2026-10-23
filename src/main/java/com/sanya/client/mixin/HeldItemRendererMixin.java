package com.sanya.client.mixin;

import com.sanya.client.SanyaClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Inject(method = "renderArm", at = @At("HEAD"))
    private void sanya$echoArmStart(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Arm arm, CallbackInfo ci) {
        if (!SanyaClient.echoHandsEnabled) return;
        matrices.push();
        float pulse = (float) Math.sin(System.nanoTime() / 260_000_000.0) * 0.012f;
        matrices.translate(arm == Arm.RIGHT ? 0.008 : -0.008, pulse, 0.0);
        matrices.scale(1.045f + pulse, 1.045f + pulse, 1.045f + pulse);
    }

    @Inject(method = "renderArm", at = @At("TAIL"))
    private void sanya$echoArmEnd(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, Arm arm, CallbackInfo ci) {
        if (SanyaClient.echoHandsEnabled) matrices.pop();
    }

    @Inject(method = "renderArmHoldingItem", at = @At("HEAD"))
    private void sanya$echoHeldArmStart(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                        float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        if (!SanyaClient.echoHandsEnabled) return;
        matrices.push();
        float pulse = (float) Math.sin(System.nanoTime() / 260_000_000.0) * 0.012f;
        matrices.translate(arm == Arm.RIGHT ? 0.008 : -0.008, pulse, 0.0);
        matrices.scale(1.045f + pulse, 1.045f + pulse, 1.045f + pulse);
    }

    @Inject(method = "renderArmHoldingItem", at = @At("TAIL"))
    private void sanya$echoHeldArmEnd(MatrixStack matrices, OrderedRenderCommandQueue queue, int light,
                                      float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        if (SanyaClient.echoHandsEnabled) matrices.pop();
    }
}
