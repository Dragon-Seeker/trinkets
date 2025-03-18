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


import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.data.EntitySlotLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dev.emi.trinkets.payload.SyncInventoryPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Syncs slot data to player's client on login
 *
 * @author C4
 */
@Mixin(PlayerManager.class)
public abstract class PlayerManagerMixin {

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerPlayerEntity;onSpawn()V"), method = "onPlayerConnect")
	private void onPlayerConnect(ClientConnection connection, ServerPlayerEntity player, ConnectedClientData clientData, CallbackInfo ci) {
		EntitySlotLoader.SERVER.sync(player);
		((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(false);
		TrinketsApi.getTrinketComponent(player).ifPresent(trinkets -> {
			Map<String, NbtCompound> tag = new HashMap<>();
			Set<TrinketInventory> inventoriesToSend = trinkets.getTrackingUpdates();

			for (TrinketInventory trinketInventory : inventoriesToSend) {
				tag.put(trinketInventory.getSlotType().getId(), trinketInventory.getSyncTag());
			}
			ServerPlayNetworking.send(player, new SyncInventoryPayload(player.getId(), Map.of(), tag));
			inventoriesToSend.clear();
		});
	}
}