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

import com.google.common.collect.Multimap;

import org.ladysnake.cca.api.v3.component.ComponentV3;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Pair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public interface TrinketComponent extends ComponentV3 {

	LivingEntity getEntity();

	/**
	 * @return A map of names to slot groups available to the entity
	 */
	Map<String, SlotGroup> getGroups();

	/**
	 * @return A map of slot group names, to slot names, to trinket inventories
	 * for the entity. Inventories will respect EAM slot count modifications for
	 * the entity.
	 */
	Map<String, Map<String, TrinketInventory>> getInventory();

	void update();

	void addTemporaryModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void addPersistentModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void removeModifiers(Multimap<String, EntityAttributeModifier> modifiers);

	void clearModifiers();

	Multimap<String, EntityAttributeModifier> getModifiers();

	/**
	 * @return Whether the predicate matches any slots available to the entity
	 */
	boolean isEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return Whether the item is in any slots available to the entity
	 */
	default boolean isEquipped(Item item) {
		return isEquipped(stack -> stack.getItem() == item);
	}

	/**
	 * @return All slots that match the provided predicate
	 */
	List<Pair<SlotReference, ItemStack>> getEquipped(Predicate<ItemStack> predicate);

	/**
	 * @return All slots that contain the provided item
	 */
	default List<Pair<SlotReference, ItemStack>> getEquipped(Item item) {
		return getEquipped(stack -> stack.getItem() == item);
	}

	/**
	 * @return All non-empty slots
	 */
	default List<Pair<SlotReference, ItemStack>> getAllEquipped() {
		return getEquipped(stack -> !stack.isEmpty());
	}

	/**
	 * Iterates over every slot available to the entity
	 */
	void forEach(BiConsumer<SlotReference, ItemStack> consumer);

	Set<TrinketInventory> getTrackingUpdates();

	void clearCachedModifiers();
}