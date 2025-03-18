package io.wispforest.tclayer.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.emi.trinkets.api.TrinketsApi;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import io.wispforest.tclayer.compat.WrappedAccessory;
import net.minecraft.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(value = AccessoriesRendererRegistry.class)
public abstract class AccessoriesRendererRegistryMixin {
    @WrapOperation(method = "getRender(Lnet/minecraft/item/Item;)Lio/wispforest/accessories/api/client/AccessoryRenderer;", at = @At(value = "INVOKE", target = "Ljava/util/Map;getOrDefault(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object alterDefaultBehavior(Map<Item, AccessoryRenderer> CACHED_RENDERERS, Object item, Object defaultRenderer, Operation<Object> operation) {
        var trinket = TrinketsApi.getTrinket((Item) item);

        if(!(trinket == TrinketsApi.getDefaultTrinket() || trinket instanceof WrappedAccessory)) {
            defaultRenderer = null;
        }

        return operation.call(CACHED_RENDERERS, item, defaultRenderer);
    }
}
