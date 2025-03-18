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

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

/**
 * A gui slot for a trinket slot, used in the survival inventory, but suited for any case
 */
public class SurvivalTrinketSlot extends Slot implements TrinketSlot {
	private final SlotGroup group;
	private final SlotType type;
	private final boolean alwaysVisible;
	private final int slotOffset;
	private final TrinketInventory trinketInventory;

	public SurvivalTrinketSlot(TrinketInventory inventory, int index, int x, int y, SlotGroup group, SlotType type, int slotOffset,
			boolean alwaysVisible) {
		super(inventory, index, x, y);
		this.group = group;
		this.type = type;
		this.slotOffset = slotOffset;
		this.alwaysVisible = alwaysVisible;
		this.trinketInventory = inventory;
	}

	@Override
	public boolean canInsert(ItemStack stack) {
		return TrinketSlot.canInsert(stack, new SlotReference(trinketInventory, slotOffset), trinketInventory.getComponent().getEntity());
	}

	@Override
	public boolean canTakeItems(PlayerEntity player) {
		ItemStack stack = this.getStack();
		return TrinketsApi.getTrinket(stack.getItem())
			.canUnequip(stack, new SlotReference(trinketInventory, slotOffset), player);
	}

	@Override
	public boolean isEnabled() {
		if (alwaysVisible) {
			if (x < 0) {
				World world = trinketInventory.getComponent().getEntity().getWorld();
				if (world.isClient) {
					MinecraftClient client = MinecraftClient.getInstance();
					Screen s = client.currentScreen;
					if (s instanceof InventoryScreen screen) {
						if (screen.getRecipeBookWidget().isOpen()) {
							return false;
						}
					}
				}
			}
			return true;
		}
		return isTrinketFocused();
	}

	@Override
	public boolean isTrinketFocused() {
		if (TrinketsClient.activeGroup == group) {
			return slotOffset == 0 || TrinketsClient.activeType == type;
		} else if (TrinketsClient.quickMoveGroup == group) {
			return slotOffset == 0 || TrinketsClient.quickMoveType == type && TrinketsClient.quickMoveTimer > 0;
		}
		return false;
	}

	@Override
	public Identifier getBackgroundIdentifier() {
		return type.getIcon();
	}

	@Override
	public SlotType getType() {
		return type;
	}
}
