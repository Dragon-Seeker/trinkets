package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.data.SlotGroupLoader;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(SlotGroupLoader.class)
public abstract class SlotGroupLoaderMixin {

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void injectTrinketsGroupingInfo(Map<Identifier, JsonObject> data, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci,
                                            @Local(name = "slotGroups", ordinal = 0) HashMap<String, SlotGroupLoader.SlotGroupBuilder> slotGroups,
                                            @Local(name = "allSlots", ordinal = 1) HashMap<String, SlotType> allSlots,
                                            @Local(name = "remainSlots") HashSet<String> remainSlots) {
        for (var groupEntry : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupData = groupEntry.getValue();
            var slots = groupData.slots;

            for (var slotEntry : slots.entrySet()) {
                var slotName = slotEntry.getKey();

                var accessorySlotName = WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(groupEntry.getKey()), slotName);

                if(!allSlots.containsKey(accessorySlotName)) continue;

                var groupName = WrappingTrinketsUtils.trinketsToAccessories_Group(groupEntry.getKey());

                var group = slotGroups.getOrDefault(groupName, null);

                if(group == null) {
                    group = new SlotGroupLoader.SlotGroupBuilder(groupName)
                            .order(0);

                    slotGroups.put(groupName, group);
                }

                group.addSlot(accessorySlotName);

                allSlots.remove(accessorySlotName);
                remainSlots.remove(accessorySlotName);
            }
        }
    }
}
