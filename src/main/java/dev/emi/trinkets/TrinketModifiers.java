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
package dev.emi.trinkets;

import com.google.common.collect.Multimap;
import dev.emi.trinkets.api.SlotAttributes;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketsAttributeModifiersComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

public class TrinketModifiers {

	//internalizes getTrinket and slotIdentifier, both which typically are generated just before the modifiers call anyway
	public static Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> get(ItemStack stack, SlotReference slot, LivingEntity entity){
		Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = TrinketsApi.getTrinket(stack.getItem()).getModifiers(stack, slot, entity, SlotAttributes.getIdentifier(slot));
		if (stack.contains(TrinketsAttributeModifiersComponent.TYPE)) {
			for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
				map.put(entry.attribute(), entry.modifier());
			}
		}
		return map;
	}

	//overload if a custom method for retrieving the trinket is used. Also exposes the slotIdentifier if custom on that is needed
	public static Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> get(Trinket trinket, ItemStack stack, SlotReference slot, LivingEntity entity, Identifier slotIdentifier){
		Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> map = trinket.getModifiers(stack, slot, entity, slotIdentifier);
		if (stack.contains(TrinketsAttributeModifiersComponent.TYPE)) {
			for (TrinketsAttributeModifiersComponent. Entry entry : stack.getOrDefault(TrinketsAttributeModifiersComponent.TYPE, TrinketsAttributeModifiersComponent.DEFAULT).modifiers()) {
				if (entry.slot().isEmpty() || entry.slot().get().equals(slot.inventory().getSlotType().getId())) {
					map.put(entry.attribute(), entry.modifier());
				}
			}
		}
		return map;
	}
}