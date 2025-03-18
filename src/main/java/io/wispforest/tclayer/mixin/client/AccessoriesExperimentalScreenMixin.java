package io.wispforest.tclayer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.tclayer.pond.SlotRenderingUtils;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccessoriesExperimentalScreen.class)
public abstract class AccessoriesExperimentalScreenMixin {

    @WrapOperation(
            method = "renderSlotTexture",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawSprite(IIIIILnet/minecraft/client/texture/Sprite;)V"))
    private static void adjustSlotRender(DrawContext instance, int x, int y, int blitOffset, int width, int height, Sprite sprite, Operation<Void> original, @Local(argsOnly = true) Slot slot, @Local(ordinal = 0) Pair<Identifier, Identifier> pair) {
        if (SlotRenderingUtils.renderSlotTexture(instance, x, y, blitOffset, width, height, sprite, slot, pair)) return;

        original.call(instance, x, y, blitOffset, width, height, sprite);
    }
}
