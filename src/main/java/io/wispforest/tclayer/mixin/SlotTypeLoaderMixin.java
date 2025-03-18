package io.wispforest.tclayer.mixin;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.data.SlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.tclayer.TCLayer;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
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

@Mixin(value = SlotTypeLoader.class, priority = 1100)
public abstract class SlotTypeLoaderMixin {

    @Unique private final Logger LOGGER = LogUtils.getLogger();

    @Unique private final Identifier EMPTY_TEXTURE = Identifier.of(TrinketsMain.MOD_ID, "gui/slots/empty.png");

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/resource/ResourceManager;Lnet/minecraft/util/profiler/Profiler;)V", at = @At(value = "INVOKE", target = "Ljava/util/HashMap;<init>()V", shift = At.Shift.AFTER, ordinal = 2))
    private void injectTrinketSpecificSlots(Map<Identifier, JsonObject> data, ResourceManager resourceManager, Profiler profiler, CallbackInfo ci, @Local(name = "builders") HashMap<String, SlotTypeLoader.SlotBuilder> builders){
        var redirects = SlotIdRedirect.getMap(TCLayer.CONFIG.slotIdRedirects());
        
        for (var groupDataEntry : SlotLoader.INSTANCE.getSlots().entrySet()) {
            var groupName = groupDataEntry.getKey();
            var groupData = groupDataEntry.getValue();

            var slots = groupData.slots;

            SlotTypeLoader.SlotBuilder builder;

            for (var entry : slots.entrySet()) {
                var redirect = redirects.get(groupName + "/" + entry.getKey());

                String accessoryType = redirect != null
                        ? redirect.key()
                        : WrappingTrinketsUtils.trinketsToAccessories_Slot(Optional.of(groupName), entry.getKey());

                var slotData = entry.getValue();

                if (builders.containsKey(accessoryType)) {
                    builder = builders.get(accessoryType);

                    var slotsCurrentSize = builder.baseAmount;

                    if(slotsCurrentSize != null && slotData.amount > slotsCurrentSize) {
                        builder.amount(slotData.amount);
                    }

                    if (redirect != null) {
                        builder.addAmount(redirect.right());
                    }
                } else {
                    builder = new SlotTypeLoader.SlotBuilder(accessoryType);

                    if (slotData.amount != -1) builder.amount(slotData.amount);

                    builder.order(slotData.order);

                    var icon = Identifier.of(slotData.icon);

                    if(!icon.equals(EMPTY_TEXTURE)) builder.icon(icon);

                    builder.dropRule(WrappingTrinketsUtils.convertDropRule(TrinketEnums.DropRule.valueOf(slotData.dropRule)));

                    builder.alternativeTranslation("trinkets.slot." + groupDataEntry.getKey() + "." + entry.getKey());

                    builders.put(accessoryType, builder);
                }

                for (String validatorPredicate : slotData.validatorPredicates) {
                    var location = Identifier.tryParse(validatorPredicate);

                    if(location == null) continue;

                    builder.validator(WrappingTrinketsUtils.trinketsToAccessories_Validators(location));
                }
            }
        }
    }
}
