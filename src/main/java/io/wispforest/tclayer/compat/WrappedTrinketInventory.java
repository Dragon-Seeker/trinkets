package io.wispforest.tclayer.compat;

import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.tclayer.pond.CosmeticLookupTogglable;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Map;

public class WrappedTrinketInventory extends TrinketInventory {

    public final AccessoriesContainer container;

    public WrappedTrinketInventory(TrinketComponent component, AccessoriesContainer container, SlotType slotType) {
        super(WrappedSlotType.of(slotType, container.capability().entity().getWorld().isClient()), component, trinketInventory -> {});

        this.container = container;
    }

    public void setOtherGroupName(String value) {
        ((WrappedSlotType) this.getSlotType()).setOtherGroupName(value);
    }

    @Override
    public Map<Identifier, EntityAttributeModifier> getModifiers() {
        return container.getModifiers();
    }

    @Override
    public Collection<EntityAttributeModifier> getModifiersByOperation(EntityAttributeModifier.Operation operation) {
        return container.getModifiersForOperation(operation);
    }

    @Override
    public void addModifier(EntityAttributeModifier modifier) {
        container.addTransientModifier(modifier);
    }

    @Override
    public void addPersistentModifier(EntityAttributeModifier modifier) {
        container.addPersistentModifier(modifier);
    }

    @Override
    public void removeModifier(Identifier location) {
        container.removeModifier(location);
    }

    @Override
    public void clearModifiers() {
        container.clearModifiers();
    }

    @Override
    public void removeCachedModifier(EntityAttributeModifier attributeModifier) {
        container.getCachedModifiers().remove(attributeModifier);
    }

    @Override
    public void clearCachedModifiers() {
        container.clearCachedModifiers();
    }

    @Override
    public void markUpdate() {
        container.markChanged(false);
    }

    //--


    @Override
    public void clear() {
        var accessories = container.getAccessories();
        var cosmetics = container.getCosmeticAccessories();

        for (int i = 0; i < accessories.size(); i++) {
            accessories.setStack(i, ItemStack.EMPTY);
            cosmetics.setStack(i, ItemStack.EMPTY);
        }

        this.markUpdate();
    }

    @Override
    public int size() {
        return container.getAccessories().size();
    }

    @Override
    public boolean isEmpty() {
        return container.getAccessories().size() != 0;
    }

    @Override
    public ItemStack getStack(int slot) {
        if(this.container.capability().entity() instanceof CosmeticLookupTogglable lookup && lookup.getLookupToggle()) {
            if(!this.container.shouldRender(slot)) return ItemStack.EMPTY;

            var accessoryStack = container.getCosmeticAccessories().getStack(slot);

            if(accessoryStack.isEmpty()) accessoryStack = container.getAccessories().getStack(slot);

            return accessoryStack;
        }

        return container.getAccessories().getStack(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        return container.getAccessories().removeStack(slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        var stacks = container.getAccessories();

        var itemStack = stacks.getStack(slot);
        stacks.setStack(slot, ItemStack.EMPTY);

        return itemStack;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        container.getAccessories().setStack(slot, stack);
    }

    //--
}
