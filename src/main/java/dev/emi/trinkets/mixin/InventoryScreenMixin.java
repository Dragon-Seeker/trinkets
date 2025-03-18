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
package dev.emi.trinkets.mixin;

import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.emi.trinkets.Point;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketScreen;
import dev.emi.trinkets.TrinketScreenManager;
import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookProvider;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Delegates drawing and slot group selection logic
 * 
 * @author Emi
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractInventoryScreen<PlayerScreenHandler> implements RecipeBookProvider, TrinketScreen {

	private InventoryScreenMixin() { super(null, null, null); }

	@Inject(at = @At("HEAD"), method = "init")
	private void init(CallbackInfo info) {
		TrinketScreenManager.init(this);
	}

	@Inject(at = @At("TAIL"), method = "handledScreenTick")
	private void tick(CallbackInfo info) {
		TrinketScreenManager.tick();
	}

	@Inject(at = @At("HEAD"), method = "render")
	private void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
		TrinketScreenManager.update(mouseX, mouseY);
	}

	@Inject(at = @At("RETURN"), method = "drawBackground")
	private void drawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo info) {
		TrinketScreenManager.drawExtraGroups(context);
	}

	@Inject(at = @At("TAIL"), method = "drawForeground")
	private void drawForeground(DrawContext context, int mouseX, int mouseY, CallbackInfo info) {
		TrinketScreenManager.drawActiveGroup(context);
	}
	
	@Inject(at = @At("HEAD"), method = "isClickOutsideBounds", cancellable = true)
	private void isClickOutsideBounds(double mouseX, double mouseY, int left, int top, int button, CallbackInfoReturnable<Boolean> info) {
		if (TrinketScreenManager.isClickInsideTrinketBounds(mouseX, mouseY)) {
			info.setReturnValue(false);
		}
	}

	@Override
	public TrinketPlayerScreenHandler trinkets$getHandler() {
		return (TrinketPlayerScreenHandler) this.handler;
	}
	
	@Override
	public Rect2i trinkets$getGroupRect(SlotGroup group) {
		Point pos = ((TrinketPlayerScreenHandler) handler).trinkets$getGroupPos(group);
		if (pos != null) {
			return new Rect2i(pos.x() - 1, pos.y() - 1, 17, 17);
		}
		return new Rect2i(0, 0, 0, 0);
	}

	@Override
	public Slot trinkets$getFocusedSlot() {
		return this.focusedSlot;
	}

	@Override
	public int trinkets$getX() {
		return this.x;
	}

	@Override
	public int trinkets$getY() {
		return this.y;
	}

	@Override
	public boolean trinkets$isRecipeBookOpen() {
		return this.getRecipeBookWidget().isOpen();
	}
}
