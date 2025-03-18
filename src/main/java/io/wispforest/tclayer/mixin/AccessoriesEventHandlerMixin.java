package io.wispforest.tclayer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.tclayer.compat.WrappedTrinket;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccessoriesEventHandler.class)
public abstract class AccessoriesEventHandlerMixin {

    @WrapOperation(
            method = "handleInvalidStacks",
            at = @At(value = "INVOKE", target = "Lio/wispforest/accessories/api/AccessoriesAPI;canInsertIntoSlot(Lnet/minecraft/item/ItemStack;Lio/wispforest/accessories/api/slot/SlotReference;)Z"))
    private static boolean tclayer$adjustCheckBehavior(ItemStack stack, SlotReference reference, Operation<Boolean> operation){
        var accessory = AccessoriesAPI.getAccessory(stack);

        if (!(accessory instanceof WrappedTrinket)) return operation.call(stack, reference);

        var slotType = reference.type();

        return AccessoriesAPI.getPredicateResults(slotType.validators(), reference.entity().getWorld(), reference.entity(), slotType, 0, stack);
    }
}
