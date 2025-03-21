package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.tclayer.TCLayer;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
import net.minecraft.entity.EntityType;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(EntitySlotLoader.class)
public abstract class EntitySlotLoaderMixin {

    @Unique
    private static final Logger TRINKET_LOGGER = LogUtils.getLogger();

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Lcom/google/common/collect/ImmutableMap;copyOf(Ljava/util/Map;)Lcom/google/common/collect/ImmutableMap;"))
    private void injectTrinketSpecificSlots(Map<Identifier, JsonObject> data, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci, @Local(name = "tempMap") HashMap<EntityType<?>, Map<String, SlotType>> tempMap){
        var loader = dev.emi.trinkets.data.EntitySlotLoader.SERVER;

        var slotTypes = SlotTypeLoader.INSTANCE.getSlotTypes(false);

        var redirects = SlotIdRedirect.getMap(TCLayer.CONFIG.slotIdRedirects());

        for (var entry : loader.slotInfo.entrySet()) {
            var innerMap = tempMap.computeIfAbsent(entry.getKey(), entityType -> new HashMap<>());

            var groupedSlots = entry.getValue();

            for (var groupEntry : groupedSlots.entrySet()) {
                var groupName = groupEntry.getKey();

                for (String slotName : groupEntry.getValue()) {
                    var redirect = redirects.get(groupName + "/" + slotName);

                    String accessoryType = redirect != null
                            ? redirect.key()
                            : WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(groupName), slotName);

                    if (innerMap.containsKey(accessoryType)) continue;

                    var slotType = slotTypes.get(accessoryType);

                    if (slotType == null) {
                        TRINKET_LOGGER.warn("Unable to locate the given slot for a given entity binding, it will be skipped: [Name: {}]", accessoryType);

                        continue;
                    }

                    innerMap.put(slotType.name(), slotType);
                }
            }
        }
    }
}
