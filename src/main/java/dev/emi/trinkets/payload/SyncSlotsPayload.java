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
package dev.emi.trinkets.payload;

import dev.emi.trinkets.TrinketsNetwork;
import dev.emi.trinkets.api.SlotGroup;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.RegistryKeys;

import java.util.HashMap;
import java.util.Map;

public record SyncSlotsPayload(Map<EntityType<?>, Map<String, SlotGroup>> map) implements CustomPayload {
	public static final PacketCodec<RegistryByteBuf, SyncSlotsPayload> CODEC = PacketCodecs.map(
			(x) -> (Map<EntityType<?>, Map<String, SlotGroup>>) new HashMap<EntityType<?>, Map<String, SlotGroup>>(x),
			PacketCodecs.registryValue(RegistryKeys.ENTITY_TYPE),
			PacketCodecs.map(HashMap::new, PacketCodecs.STRING, PacketCodecs.NBT_COMPOUND.xmap(SlotGroup::read, (x) -> {
				NbtCompound nbt = new NbtCompound();
				x.write(nbt);
				return nbt;
			}))
			).xmap(SyncSlotsPayload::new, SyncSlotsPayload::map);

	@Override
	public Id<? extends CustomPayload> getId() {
		return TrinketsNetwork.SYNC_SLOTS;
	}
}