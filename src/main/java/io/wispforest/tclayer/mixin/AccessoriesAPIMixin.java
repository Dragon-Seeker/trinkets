package io.wispforest.tclayer.mixin;

import dev.emi.trinkets.api.TrinketsAttributeModifiersComponent;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AccessoriesAPI.class)
public abstract class AccessoriesAPIMixin {
    @Inject(method = "getAttributeModifiers(Lnet/minecraft/item/ItemStack;Lnet/minecraft/entity/LivingEntity;Ljava/lang/String;IZ)Lio/wispforest/accessories/api/attributes/AccessoryAttributeBuilder;", at = @At("RETURN"))
    private static void trinkets$getDataAttributeModifiers(ItemStack stack, @Nullable LivingEntity entity, String slotName, int slot, boolean hideTooltipIfDisabled, CallbackInfoReturnable<AccessoryAttributeBuilder> cir) {
        if (!stack.contains(TrinketsAttributeModifiersComponent.TYPE)) return;

        var builder = cir.getReturnValue();

        for (var entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
            if (entry.slot().isEmpty()) {
                builder.addExclusive(entry.attribute(), entry.modifier());
            } else if(entity != null) {
                var group = WrappingTrinketsUtils.getGroup(entity.getWorld(), slotName);

                var slotId = WrappingTrinketsUtils.accessoriesToTrinkets_Group(group.name()) + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(slotName);

                if(entry.slot().get().equals(slotId)) builder.addExclusive(entry.attribute(), entry.modifier());
            }
        }
    }
}
