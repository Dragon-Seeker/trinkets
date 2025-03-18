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

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public interface TrinketSlot {

	public boolean isTrinketFocused();

	public Identifier getBackgroundIdentifier();

	public SlotType getType();
	
	public static boolean canInsert(ItemStack stack, SlotReference slotRef, LivingEntity entity) {
		boolean res = TrinketsApi.evaluatePredicateSet(slotRef.inventory().getSlotType().getValidatorPredicates(),
			stack, slotRef, entity);

		if (res) {
			return TrinketsApi.getTrinket(stack.getItem()).canEquip(stack, slotRef, entity);
		}

		return false;
	}
}
