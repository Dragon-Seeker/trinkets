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
package dev.emi.trinkets.compat;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import dev.emi.trinkets.TrinketsClient;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.util.math.Rect2i;

public class TrinketsExclusionAreas {

	public static List<Rect2i> create(Screen screen) {
		if (screen instanceof TrinketScreen trinketScreen) {
			if (screen instanceof CreativeInventoryScreen creativeInventoryScreen &&
				!creativeInventoryScreen.isInventoryTabSelected()) {
				return List.of();
			}
			List<Rect2i> rects = new ArrayList<>();
			int x = trinketScreen.trinkets$getX();
			int y = trinketScreen.trinkets$getY();
			TrinketPlayerScreenHandler handler = trinketScreen.trinkets$getHandler();
			int groupCount = handler.trinkets$getGroupCount();
			if (groupCount <= 0 || trinketScreen.trinkets$isRecipeBookOpen()) {
				return List.of();
			}

			if (TrinketsClient.activeGroup != null) {
				Rect2i rect = TrinketScreenManager.currentBounds;
				rects.add(
					new Rect2i(rect.getX() + x, rect.getY() + y, rect.getWidth(),
						rect.getHeight()));
			}
			int width = groupCount / 4;
			int height = groupCount % 4;
			if (width > 0) {
				rects.add(new Rect2i(-4 - 18 * width + x, y, 7 + 18 * width, 86));
			}

			if (height > 0) {
				rects.add(new Rect2i(-22 - 18 * width + x, y, 25, 14 + 18 * height));
			}
			return rects;
		}
		return List.of();
	}
}
