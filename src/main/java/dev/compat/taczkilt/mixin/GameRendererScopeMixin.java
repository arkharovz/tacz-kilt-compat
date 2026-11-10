package dev.compat.taczkilt.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.scope.ScopeLensRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Re-implementation of the two TACZ 1.2.0 injectors into GameRenderer#renderLevel that fail under Kilt
 * (tacz.mixins.json:client.GameRendererMixin -> "Scanned 0 target(s)").
 * Same targets and same bodies as in TACZ, but resolved through OUR refmap instead of Kilt's.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererScopeMixin {

    // TACZ: tacz$beginScopeFrame, @At("HEAD")
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void taczKilt$beginScopeFrame(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        ScopeLensRenderer.beginFrame();
    }

    // TACZ: tacz$drawShaderScopeLenses, INVOKE ProfilerFiller#popPush(String), ordinal = 2.
    // Ordinal 2 counts Forge's extra popPush("forge_render_last") and matches nothing on Fabric (log: Scanned 0 target(s)),
    // so we hook popPush("hand") instead: right after the level is rendered, before the hand.
    @Inject(
        method = "renderLevel",
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
            args = "ldc=hand"
        )
    )
    private void taczKilt$drawShaderScopeLenses(float partialTick, long finishTimeNano, PoseStack poseStack, CallbackInfo ci) {
        ScopeLensRenderer.render();
    }
}
