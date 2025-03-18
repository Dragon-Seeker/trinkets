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
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.data.EntitySlotLoader;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.Map;

public class TrinketsClient implements ClientModInitializer {
	public static SlotGroup activeGroup;
	public static SlotType activeType;
	public static SlotGroup quickMoveGroup;
	public static SlotType quickMoveType;
	public static int quickMoveTimer;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_INVENTORY, (payload, context) -> {
			MinecraftClient client = context.client();
			Entity entity = client.world.getEntityById(payload.entityId());
			if (entity instanceof LivingEntity) {
				TrinketsApi.getTrinketComponent((LivingEntity) entity).ifPresent(trinkets -> {
					for (Map.Entry<String, NbtCompound> entry : payload.inventoryUpdates().entrySet()) {
						String[] split = entry.getKey().split("/");
						String group = split[0];
						String slot = split[1];
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
							if (inv != null) {
								inv.applySyncTag(entry.getValue());
							}
						}
					}

					if (entity instanceof PlayerEntity && ((PlayerEntity) entity).playerScreenHandler instanceof TrinketPlayerScreenHandler screenHandler) {
						screenHandler.trinkets$updateTrinketSlots(false);
						if (TrinketScreenManager.currentScreen != null) {
							TrinketScreenManager.currentScreen.trinkets$updateTrinketSlots();
						}
					}

					for (Map.Entry<String, ItemStack> entry : payload.contentUpdates().entrySet()) {
						String[] split = entry.getKey().split("/");
						String group = split[0];
						String slot = split[1];
						int index = Integer.parseInt(split[2]);
						Map<String, TrinketInventory> slots = trinkets.getInventory().get(group);
						if (slots != null) {
							TrinketInventory inv = slots.get(slot);
							if (inv != null && index < inv.size()) {
								inv.setStack(index, entry.getValue());
							}
						}
					}
				});
			}
		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.SYNC_SLOTS, (payload, context) -> {

			EntitySlotLoader.CLIENT.setSlots(payload.map());
			ClientPlayerEntity player = context.player();

			if (player != null) {
				((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(true);

				MinecraftClient client = context.client();
				if (client.currentScreen instanceof TrinketScreen trinketScreen) {
					trinketScreen.trinkets$updateTrinketSlots();
				}

				for (AbstractClientPlayerEntity clientWorldPlayer : player.clientWorld.getPlayers()) {
					((TrinketPlayerScreenHandler) clientWorldPlayer.playerScreenHandler).trinkets$updateTrinketSlots(true);
				}
			}

		});
		ClientPlayNetworking.registerGlobalReceiver(TrinketsNetwork.BREAK, (payload, context) -> {

			MinecraftClient client = context.client();
			Entity e = client.world.getEntityById(payload.entityId());
			if (e instanceof LivingEntity entity) {
				TrinketsApi.getTrinketComponent(entity).ifPresent(comp -> {
					Map<String, TrinketInventory> groupMap = comp.getInventory().get(payload.group());
					if (groupMap != null) {
						TrinketInventory inv = groupMap.get(payload.slot());
						if (payload.index() < inv.size()) {
							ItemStack stack = inv.getStack(payload.index());
							SlotReference ref = new SlotReference(inv, payload.index());
							Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
							trinket.onBreak(stack, ref, entity);
						}
					}
				});
			}

		});
	}
}