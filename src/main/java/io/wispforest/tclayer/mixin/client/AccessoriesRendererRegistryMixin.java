package io.wispforest.tclayer.mixin.client;

import com.google.common.collect.BiMap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.api.TrinketsApi;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.tclayer.compat.WrappedAccessory;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AccessoriesRendererRegistry.class)
public abstract class AccessoriesRendererRegistryMixin {
    @WrapOperation(method = "getRenderer(Lnet/minecraft/item/Item;)Lio/wispforest/accessories/api/client/AccessoryRenderer;", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/BiMap;containsKey(Ljava/lang/Object;)Z"))
    private static boolean alterDefaultBehavior(BiMap map, Object key, Operation<Boolean> operation, @Local(argsOnly = true) Item item) {
        var trinket = TrinketsApi.getTrinket(item);

        if(!(trinket == TrinketsApi.getDefaultTrinket() || trinket instanceof WrappedAccessory)) {
            return true;
        }

        return operation.call(map, key);
    }
}
