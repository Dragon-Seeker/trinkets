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
package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketEnums.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.impl.event.WrappedEvent;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;

public interface TrinketDropCallback {
	Event<TrinketDropCallback> EVENT = new WrappedEvent<>(OnDropCallback.EVENT, callback -> {
		return (dropRule, stack, reference, damageSource) -> {
			var slotReference = WrappingTrinketsUtils.createTrinketsReference(reference);

			if(slotReference.isEmpty()) return io.wispforest.accessories.api.DropRule.DEFAULT;

			return WrappingTrinketsUtils.convertDropRule(callback.drop(WrappingTrinketsUtils.convertDropRule(dropRule), stack, slotReference.get(), reference.entity()));
		};
	}, onDropCallbackEvent -> {
		return (rule, stack, ref, entity) -> {
			var reference = WrappingTrinketsUtils.createAccessoriesReference(ref).get();
			var accessoryRule = WrappingTrinketsUtils.convertDropRule(rule);

			var source = entity.getRecentDamageSource();

			if(source == null) source = entity.getWorld().getDamageSources().genericKill();

			var value = onDropCallbackEvent.invoker().onDrop(accessoryRule, stack, reference, source);

			return value != null ? WrappingTrinketsUtils.convertDropRule(value) : TrinketEnums.DropRule.DEFAULT;
		};
	});

	DropRule drop(DropRule rule, ItemStack stack, SlotReference ref, LivingEntity entity);
}