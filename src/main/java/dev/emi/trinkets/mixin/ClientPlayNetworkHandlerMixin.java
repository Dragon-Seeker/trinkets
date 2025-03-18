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

import com.llamalad7.mixinextras.sugar.Local;
import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.api.TrinketInventory;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Copies slot EAMs to players client-side when changing dimensions
 *
 * @author C4
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setId(I)V"), method = "onPlayerRespawn")
    private void onPlayerRespawn(PlayerRespawnS2CPacket packet, CallbackInfo info, @Local(ordinal = 0) ClientPlayerEntity clientPlayerEntity, @Local(ordinal = 1) ClientPlayerEntity clientPlayerEntity2)  {
        if (true) return;
        if (packet.hasFlag(PlayerRespawnS2CPacket.KEEP_ATTRIBUTES)) {
            TrinketInventory.copyFrom(clientPlayerEntity, clientPlayerEntity2);
            ((TrinketPlayerScreenHandler) clientPlayerEntity2.playerScreenHandler).trinkets$updateTrinketSlots(false);
        }
    }
}
