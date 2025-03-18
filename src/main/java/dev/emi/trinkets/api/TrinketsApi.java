/*
 * MIT License
 *
 * Copyright (c) 2019 Emily Rose Ploszaj
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Function3;
import com.mojang.logging.LogUtils;
import dev.emi.trinkets.TrinketModifiers;
import dev.emi.trinkets.TrinketSlotTarget;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.payload.BreakPayload;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.slot.EntityBasedPredicate;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.tclayer.compat.*;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistryV3;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.util.TriState;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.function.Consumer;

import net.minecraft.world.World;
import org.slf4j.Logger;

public class TrinketsApi {
	public static final ComponentKey<TrinketComponent> TRINKET_COMPONENT = ComponentRegistryV3.INSTANCE
			.getOrCreate(Identifier.of(TrinketsMain.MOD_ID, "trinkets"), TrinketComponent.class);
	private static final Map<Identifier, Function3<ItemStack, SlotReference, LivingEntity, TriState>> PREDICATES = new HashMap<>();

	private static final Map<Item, Trinket> TRINKETS = new HashMap<>();
	private static final Trinket DEFAULT_TRINKET;

	/**
	 * Registers a trinket for the provided item, {@link TrinketItem} will do this
	 * automatically.
	 */
	public static void registerTrinket(Item item, Trinket trinket) {
		TRINKETS.put(item, trinket);
	}

	public static Trinket getTrinket(Item item) {
		var trinket = TRINKETS.get(item);

		if(trinket == null) {
			var accessory = AccessoriesAPI.getAccessory(item);

			if(accessory != null) trinket = new WrappedAccessory(accessory);
		}

		//TODO: Maybe check for valid slots if any and return different wrapped accessory as a way of indicating this item is for sure a valid trinket for things like Compound Accessories
		if(trinket == null) return DEFAULT_TRINKET;

		return trinket;
	}

	public static Trinket getDefaultTrinket() {
		return DEFAULT_TRINKET;
	}

	/**
	 * @return The trinket component for this entity, if available
	 */
	public static Optional<TrinketComponent> getTrinketComponent(LivingEntity livingEntity) {
		if(livingEntity == null) return Optional.empty();

		var capability = AccessoriesCapability.get(livingEntity);

		return Optional.of(capability != null ? new WrappedTrinketComponent(livingEntity) : new EmptyComponent(livingEntity));
	}

	/**
	 * Called to sync a trinket breaking event with clients. Should generally be
	 * called in the callback of {@link ItemStack#damage(int, ServerWorld, ServerPlayerEntity, Consumer)}
	 */
	public static void onTrinketBroken(ItemStack stack, SlotReference ref, LivingEntity entity) {
		var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

		var reference = io.wispforest.accessories.api.slot.SlotReference.of(entity, slotName, ref.index());

		AccessoriesAPI.breakStack(reference);
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getPlayerSlots(World)}
	 * @return A map of slot group names to slot groups available for players
	 */
	@Deprecated
	public static Map<String, SlotGroup> getPlayerSlots() {
		return getEntitySlots(EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(World world) {
		return getEntitySlots(world, EntityType.PLAYER);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for players
	 */
	public static Map<String, SlotGroup> getPlayerSlots(PlayerEntity player) {
		return getEntitySlots(player);
	}

	/**
	 * @deprecated Use world-sensitive alternative {@link TrinketsApi#getEntitySlots(World, EntityType)}
	 * @return A map of slot group names to slot groups available for the provided
	 * entity type
	 */
	@Deprecated
	public static Map<String, SlotGroup> getEntitySlots(EntityType<?> type) {
		EntitySlotLoader loader = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity type
	 */
	public static Map<String, SlotGroup> getEntitySlots(World world, EntityType<?> type) {
		EntitySlotLoader loader = world.isClient() ? EntitySlotLoader.CLIENT : EntitySlotLoader.SERVER;
		return loader.getEntitySlots(type);
	}

	/**
	 * @return A sided map of slot group names to slot groups available for the provided
	 * entity
	 */
	public static Map<String, SlotGroup> getEntitySlots(Entity entity) {
		if (entity != null) {
			return getEntitySlots(entity.getWorld(), entity.getType());
		}
		return ImmutableMap.of();
	}

	/**
	 * Registers a predicate to be referenced in slot data
	 */
	public static void registerTrinketPredicate(Identifier id, Function3<ItemStack, SlotReference, LivingEntity, TriState> predicate) {
		PREDICATES.put(id, predicate);

		AccessoriesAPI.registerPredicate(id, new SafeSlotBasedPredicate(id, predicate));
	}

	public static Optional<Function3<ItemStack, SlotReference, LivingEntity, TriState>> getTrinketPredicate(Identifier id) {
		return Optional.ofNullable(PREDICATES.get(id));
	}

	public static boolean evaluatePredicateSet(Set<Identifier> set, ItemStack stack, SlotReference ref, LivingEntity entity) {
		var convertedSet = new HashSet<Identifier>();

		for (var location : set) {
			var converetdLocation = switch (location.toString()){
				case "trinkets:all" -> Accessories.of("all");
				case "trinkets:none" -> Accessories.of("none");
				case "trinkets:tag" -> Accessories.of("tag");
				case "trinkets:relevant" -> Accessories.of("relevant");
				default -> location;
			};

			convertedSet.add(converetdLocation);
		}

		var slotName = ((WrappedTrinketInventory) ref.inventory()).container.getSlotName();

		var slotType = SlotTypeLoader.getSlotType(entity.getWorld(), slotName);

		if(slotType == null) {
			throw new IllegalStateException("Unable to get a SlotType using the WrappedTrinketInventory from the SlotTypeLoader! [Name: " + slotName +"]");
		}

		return AccessoriesAPI.getPredicateResults(convertedSet, entity.getWorld(), entity, slotType, ref.index(), stack);
	}

	public static Enchantment.Definition withTrinketSlots(Enchantment.Definition definition, Set<String> slots) {
		Enchantment.Definition def = new Enchantment.Definition(definition.supportedItems(), definition.primaryItems(), definition.weight(), definition.maxLevel(),
				definition.minCost(), definition.maxCost(), definition.anvilCost(), definition.slots());

		((TrinketSlotTarget) (Object) def).trinkets$slots(slots);
		return def;
	}

	static {
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "all"), (stack, ref, entity) -> TriState.TRUE);
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "none"), (stack, ref, entity) -> TriState.FALSE);
		TagKey<Item> trinketsAll = TagKey.of(RegistryKeys.ITEM, Identifier.of("trinkets", "all"));

		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "tag"), (stack, ref, entity) -> {
			SlotType slot = ref.inventory().getSlotType();
			TagKey<Item> tag = TagKey.of(RegistryKeys.ITEM, Identifier.of("trinkets", slot.getId()));

			if (stack.isIn(tag) || stack.isIn(trinketsAll)) {
				return TriState.TRUE;
			}
			return TriState.DEFAULT;
		});
		TrinketsApi.registerTrinketPredicate(Identifier.of("trinkets", "relevant"), (stack, ref, entity) -> {
			var reference = io.wispforest.accessories.api.slot.SlotReference.of(entity, ref.inventory().getSlotType().getName(), ref.index());
			var builder = new AccessoryAttributeBuilder(reference);

			var accessory = AccessoriesAPI.getOrDefaultAccessory(stack);

			if(accessory != null) accessory.getDynamicModifiers(stack, reference, builder);

			if (!builder.getAttributeModifiers(false).isEmpty()) return TriState.TRUE;
			return TriState.DEFAULT;
		});
		DEFAULT_TRINKET = new WrappedAccessory(AccessoriesAPI.defaultAccessory());
	}

	private final static class SafeSlotBasedPredicate implements EntityBasedPredicate {
		private static final Logger LOGGER = LogUtils.getLogger();
		private boolean hasErrored = false;

		private final Identifier location;
		private final Function3<ItemStack, SlotReference, LivingEntity, TriState> trinketPredicate;

		public SafeSlotBasedPredicate(Identifier location, Function3<ItemStack, SlotReference, LivingEntity, TriState> trinketPredicate) {
			this.location = location;
			this.trinketPredicate = trinketPredicate;
		}

		@Override
		public TriState isValid(World level, @Nullable LivingEntity entity, io.wispforest.accessories.api.slot.SlotType slotType, int slot, ItemStack stack) {
			if(hasErrored) return TriState.DEFAULT;

			try {
				return this.trinketPredicate.apply(stack, new SlotReference(new CursedTrinketInventory(slotType, level.isClient()), slot), entity);
			} catch (Exception e) {
				this.hasErrored = true;
				LOGGER.warn("Unable to handle Trinket Slot Predicate converted to Accessories Slot Predicate due to fundamental incompatibility, issues may be present with it! [Slot: {}, Predicate ID: {}]", slotType.name(), this.location, e);
			}

			return TriState.DEFAULT;
		}
	}

	private static final class CursedTrinketInventory extends TrinketInventory {
		public CursedTrinketInventory(io.wispforest.accessories.api.slot.SlotType slotType, boolean isClientSide) {
			super(WrappedSlotType.of(slotType, isClientSide), null, inv -> {});
		}
	}
}