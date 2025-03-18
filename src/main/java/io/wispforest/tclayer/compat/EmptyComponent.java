package io.wispforest.tclayer.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketComponent;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class EmptyComponent implements TrinketComponent {

    private final LivingEntity livingEntity;

    public EmptyComponent(LivingEntity livingEntity){
        this.livingEntity = livingEntity;
    }

    @Override
    public LivingEntity entity() {
        return livingEntity;
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return Map.of();
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        return Map.of();
    }

    @Override
    public void update() {}

    @Override
    public void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers) {}

    @Override
    public void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers) {}

    @Override
    public void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers) {}

    @Override
    public void clearModifiers() {}

    @Override
    public Multimap<String, EntityAttributeModifier> getModifiers() {
        return HashMultimap.create();
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return false;
    }

    @Override
    public List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        return List.of();
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {}

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return Set.of();
    }

    @Override
    public void clearCachedModifiers() {}

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {}

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {}
}
