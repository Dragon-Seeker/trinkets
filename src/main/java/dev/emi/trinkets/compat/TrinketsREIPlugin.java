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

import dev.emi.trinkets.TrinketScreen;
import java.util.List;
import java.util.stream.Collectors;

import dev.emi.trinkets.compat.TrinketsExclusionAreas;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ExclusionZones;
import net.minecraft.client.gui.screen.Screen;

public class TrinketsREIPlugin implements REIClientPlugin {

	@Override
	public void registerExclusionZones(ExclusionZones zones) {
		zones.register(TrinketScreen.class, screen -> {
			if (screen instanceof Screen guiScreen) {
				return TrinketsExclusionAreas.create(guiScreen).stream().map(
					rect2i -> new Rectangle(rect2i.getX(), rect2i.getY(), rect2i.getWidth(),
						rect2i.getHeight())).collect(Collectors.toList());
			}
			return List.of();
		});
	}
}
