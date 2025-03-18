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

import java.util.List;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for putting methods onto the player's screen handler
 */
public interface TrinketPlayerScreenHandler {

	/**
	 * Called to inform the player's slot handler that it needs to remove and re-add its trinket slots to reflect new changes
	 */
	void trinkets$updateTrinketSlots(boolean slotsChanged);

	int trinkets$getGroupNum(SlotGroup group);

	@Nullable
	Point trinkets$getGroupPos(SlotGroup group);

	@NotNull
	List<Point> trinkets$getSlotHeights(SlotGroup group);

	@Nullable
	Point trinkets$getSlotHeight(SlotGroup group, int i);

	@NotNull
	List<SlotType> trinkets$getSlotTypes(SlotGroup group);

	int trinkets$getSlotWidth(SlotGroup group);

	int trinkets$getGroupCount();

	int trinkets$getTrinketSlotStart();

	int trinkets$getTrinketSlotEnd();
}
