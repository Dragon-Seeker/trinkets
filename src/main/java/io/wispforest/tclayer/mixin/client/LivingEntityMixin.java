package io.wispforest.tclayer.mixin.client;

import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.tclayer.pond.CosmeticLookupTogglable;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements CosmeticLookupTogglable {

    @Unique
    private boolean tclayer$cosmeticLookupToggle;

    @Override
    public void setLookupToggle(boolean value) {
        var capability = AccessoriesCapability.get((LivingEntity)(Object) this);

        if(capability == null) {
            this.tclayer$cosmeticLookupToggle = false;

            return;
        }

        this.tclayer$cosmeticLookupToggle = value;
    }

    @Override
    public boolean getLookupToggle() {
        if(!((LivingEntity)(Object) this).getWorld().isClient()) return false;

        return tclayer$cosmeticLookupToggle;
    }
}
