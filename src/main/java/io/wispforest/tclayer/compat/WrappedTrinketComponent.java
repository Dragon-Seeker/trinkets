package io.wispforest.tclayer.compat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.*;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.NbtMapCarrier;
import io.wispforest.accessories.impl.AccessoriesHolderImpl;
import io.wispforest.endec.SerializationContext;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.tclayer.OuterGroupMap;
import net.fabricmc.fabric.api.util.NbtType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public record WrappedTrinketComponent(LivingEntity entity) implements TrinketComponent {

    private static final Logger LOGGER = LogUtils.getLogger();

    public AccessoriesCapability capability() {
        return entity.accessoriesCapability();
    }

    @Override
    public Map<String, SlotGroup> getGroups() {
        return TrinketsApi.getEntitySlots(entity().getWorld(), entity().getType());
    }

    @Override
    public Map<String, Map<String, TrinketInventory>> getInventory() {
        var entity = this.entity();

        return new OuterGroupMap(WrappingTrinketsUtils.getGroupedSlots(entity.getWorld().isClient(), entity.getType()),
                this,
                capability(),
                (additionalMsg) -> {
                    LOGGER.warn("Unable to get some value leading to an error, here comes the dumping data!");
                    LOGGER.warn("Entity: {}", this.entity());
                    LOGGER.warn("Entity Slots: {}", EntitySlotLoader.getEntitySlots(this.entity()));
                    LOGGER.warn("Current Containers: {}", this.entity().accessoriesCapability().getContainers());
                    LOGGER.warn("More Info: ({})", additionalMsg);
                });
    }

    @Override
    public void update() {
        capability().updateContainers();
    }

    @Override
    public void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        capability().addTransientSlotModifiers(modifiers);
    }

    @Override
    public void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        capability().addPersistentSlotModifiers(modifiers);
    }

    @Override
    public void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers) {
        capability().removeSlotModifiers(modifiers);
    }

    @Override
    public void clearModifiers() {
        capability().clearSlotModifiers();
    }

    @Override
    public Multimap<String, EntityAttributeModifier> getModifiers() {
        return capability().getSlotModifiers();
    }

    @Override
    public boolean isEquipped(Predicate<ItemStack> predicate) {
        return capability().isEquipped(predicate);
    }

    @Override
    public List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate) {
        var equipped = capability().getEquipped(predicate);

        return equipped.stream()
                .map(slotResult -> {
                    var reference = WrappingTrinketsUtils.createTrinketsReference(slotResult.reference());

                    return reference.map(slotReference -> new Pair<>(
                            slotReference,
                            slotResult.stack()
                    )).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public List<Pair<SlotReference, ItemStack>> getAllEquipped() {
        return capability().getAllEquipped().stream()
                .map(slotResult -> {
                    var reference = WrappingTrinketsUtils.createTrinketsReference(slotResult.reference());

                    return reference.map(slotReference -> new Pair<>(
                            slotReference,
                            slotResult.stack()
                    )).orElse(null);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public void forEach(BiConsumer<SlotReference, ItemStack> consumer) {
        for (var tuple : this.getAllEquipped()) {
            consumer.accept(tuple.getLeft(), tuple.getRight());
        }
    }

    @Override
    public Set<TrinketInventory> getTrackingUpdates() {
        return null;
    }

    @Override
    public void clearCachedModifiers() {
        capability().clearCachedSlotModifiers();
    }

    @Override
    public void readFromNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        if (tag.contains("data_written_by_accessories")) return;

        var holder = this.capability().getHolder();

        var dropped = ((AccessoriesHolderImpl) holder).invalidStacks;
        for (String groupKey : tag.getKeys()) {
            NbtCompound groupTag = tag.getCompound(groupKey);
            if (groupTag != null) {
                Map<String, TrinketInventory> groupSlots = this.getInventory().get(groupKey);
                if (groupSlots != null) {
                    for (String slotKey : groupTag.getKeys()) {
                        NbtCompound slotTag = groupTag.getCompound(slotKey);
                        NbtList list = slotTag.getList("Items", NbtElement.COMPOUND_TYPE);
                        TrinketInventory inv = groupSlots.get(slotKey);

                        if (inv != null) {
                            inv.fromTag(slotTag.getCompound("Metadata"));
                        }

                        for (int i = 0; i < list.size(); i++) {
                            NbtCompound c = list.getCompound(i);
                            ItemStack stack = ItemStack.fromNbtOrEmpty(lookup, c);
                            if (inv != null && i < inv.size()) {
                                inv.setStack(i, stack);
                            } else {
                                dropped.add(stack);
                            }
                        }
                    }
                } else {
                    for (String slotKey : groupTag.getKeys()) {
                        NbtCompound slotTag = groupTag.getCompound(slotKey);
                        NbtList list = slotTag.getList("Items", NbtElement.COMPOUND_TYPE);
                        for (int i = 0; i < list.size(); i++) {
                            NbtCompound c = list.getCompound(i);
                            dropped.add(ItemStack.fromNbtOrEmpty(lookup, c));
                        }
                    }
                }
            }
        }

        for (var entryRef : this.capability().getAllEquipped()) {
            var reference = entryRef.reference();
            var slotType = reference.type();

            if (AccessoriesAPI.getPredicateResults(slotType.validators(), reference.entity().getWorld(), reference.entity(), slotType, 0, entryRef.stack()))
                continue;

            dropped.add(entryRef.stack().copy());

            entryRef.reference().setStack(ItemStack.EMPTY);
        }

        // Do not decode attribute modifiers stuff as things maybe incorrect
        if (true) return;

        Multimap<String, EntityAttributeModifier> slotMap = HashMultimap.create();
        this.forEach((ref, stack) -> {
            if (!stack.isEmpty()) {
                Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketModifiers.get(stack, ref, this.entity());
                for (RegistryEntry<EntityAttribute> entityAttribute : map.keySet()) {
                    if (entityAttribute.hasKeyAndValue() && entityAttribute.value() instanceof SlotAttributes.SlotEntityAttribute slotEntityAttribute) {
                        slotMap.putAll(slotEntityAttribute.slot, map.get(entityAttribute));
                    }
                }
            }
        });
        for (Map.Entry<String, Map<String, TrinketInventory>> groupEntry : this.getInventory().entrySet()) {
            for (Map.Entry<String, TrinketInventory> slotEntry : groupEntry.getValue().entrySet()) {
                String group = groupEntry.getKey();
                String slot = slotEntry.getKey();
                String key = group + "/" + slot;
                Collection<EntityAttributeModifier> modifiers = slotMap.get(key);
                TrinketInventory inventory = slotEntry.getValue();
                for (EntityAttributeModifier modifier : modifiers) {
                    inventory.removeCachedModifier(modifier);
                }
                inventory.clearCachedModifiers();
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag, RegistryWrapper.WrapperLookup lookup) {
        tag.put("data_written_by_accessories", new NbtCompound());

        for (Map.Entry<String, Map<String, TrinketInventory>> group : this.getInventory().entrySet()) {
            NbtCompound groupTag = new NbtCompound();
            for (Map.Entry<String, TrinketInventory> slot : group.getValue().entrySet()) {
                NbtCompound slotTag = new NbtCompound();
                NbtList list = new NbtList();
                TrinketInventory inv = slot.getValue();
                for (int i = 0; i < inv.size(); i++) {
                    NbtCompound c = (NbtCompound) inv.getStack(i).encodeAllowEmpty(lookup);
                    list.add(c);
                }
                slotTag.put("Metadata", inv.toTag());
                slotTag.put("Items", list);
                groupTag.put(slot.getKey(), slotTag);
            }
            tag.put(group.getKey(), groupTag);
        }
    }
}
