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

import dev.emi.trinkets.api.SlotType;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen.CreativeSlot;
import net.minecraft.util.Identifier;

/**
 * A gui slot for a trinket slot in the creative inventory
 */
public class CreativeTrinketSlot extends CreativeSlot implements TrinketSlot {
	private final SurvivalTrinketSlot original;

	public CreativeTrinketSlot(SurvivalTrinketSlot original, int s, int x, int y) {
		super(original, s, x, y);
		this.original = original;
	}

	@Override
	public boolean isTrinketFocused() {
		return original.isTrinketFocused();
	}

	@Override
	public Identifier getBackgroundIdentifier() {
		return original.getBackgroundIdentifier();
	}

	@Override
	public SlotType getType() {
		return original.getType();
	}
}
