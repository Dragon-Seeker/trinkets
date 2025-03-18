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

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class SlotAttributes {
	private static final Map<String, Identifier> CACHED_IDS = Maps.newHashMap();
	private static final Map<String, RegistryEntry<EntityAttribute>> CACHED_ATTRIBUTES = Maps.newHashMap();

	/**
	 * Adds an Entity Attribute Modifier for slot count to the provided multimap
	 */
	public static void addSlotModifier(Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map, String slot, Identifier identifier, double amount,
			EntityAttributeModifier.Operation operation) {
		CACHED_ATTRIBUTES.putIfAbsent(slot, RegistryEntry.of(new SlotEntityAttribute(slot)));
		map.put(CACHED_ATTRIBUTES.get(slot), new EntityAttributeModifier(identifier, amount, operation));
	}

	public static Identifier getIdentifier(SlotReference ref) {
		String key = ref.getId();
		return CACHED_IDS.computeIfAbsent(key, Identifier::of);
	}

	public static class SlotEntityAttribute extends EntityAttribute {
		public String slot;

		private SlotEntityAttribute(String slot) {
			super("trinkets.slot." + slot.replace("/", "."), 0);
			this.slot = slot;
		}
	}
}