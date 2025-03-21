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
package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.Optional;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.component.ComponentType;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;

public record TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
	public static final TrinketsAttributeModifiersComponent DEFAULT = new TrinketsAttributeModifiersComponent(List.of(), true);
	private static final Codec<TrinketsAttributeModifiersComponent> BASE_CODEC = RecordCodecBuilder.create((instance) -> {
		return instance.group(
						Entry.CODEC.listOf().fieldOf("modifiers").forGetter(TrinketsAttributeModifiersComponent::modifiers),
						Codec.BOOL.optionalFieldOf("show_in_tooltip", true).forGetter(TrinketsAttributeModifiersComponent::showInTooltip)
				).apply(instance, TrinketsAttributeModifiersComponent::new);
	});
	public static final Codec<TrinketsAttributeModifiersComponent> CODEC = Codec.withAlternative(BASE_CODEC, Entry.CODEC.listOf(), (attributeModifiers) -> {
		return new TrinketsAttributeModifiersComponent(attributeModifiers, true);
	});

	public static final PacketCodec<RegistryByteBuf, TrinketsAttributeModifiersComponent> PACKET_CODEC = PacketCodec.tuple(
			Entry.PACKET_CODEC.collect(PacketCodecs.toList()),
			TrinketsAttributeModifiersComponent::modifiers,
			PacketCodecs.BOOL,
			TrinketsAttributeModifiersComponent::showInTooltip,
			TrinketsAttributeModifiersComponent::new);

	public static final ComponentType<TrinketsAttributeModifiersComponent> TYPE = ComponentType.<TrinketsAttributeModifiersComponent>builder().codec(CODEC).packetCodec(PACKET_CODEC).build();

	public TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
		this.modifiers = modifiers;
		this.showInTooltip = showInTooltip;
	}

	public TrinketsAttributeModifiersComponent withShowInTooltip(boolean showInTooltip) {
		return new TrinketsAttributeModifiersComponent(this.modifiers, showInTooltip);
	}

	public List<Entry> modifiers() {
		return this.modifiers;
	}

	public boolean showInTooltip() {
		return this.showInTooltip;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

		Builder() {
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier) {
			return add(attribute, modifier, Optional.empty());
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, String slot) {
			return add(attribute, modifier, Optional.of(slot));
		}

		public Builder add(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, Optional<String> slot) {
			this.entries.add(new Entry (attribute, modifier, slot));
			return this;
		}

		public TrinketsAttributeModifiersComponent build() {
			return new TrinketsAttributeModifiersComponent(this.entries.build(), true);
		}
	}

	public record Entry(RegistryEntry<EntityAttribute> attribute, EntityAttributeModifier modifier, Optional<String> slot) {
		private static final Endec<RegistryEntry<EntityAttribute>> ATTRIBUTE_ENDEC = MinecraftEndecs.IDENTIFIER.xmapWithContext(
				(context, attributeType) -> {
					if(attributeType.getNamespace().equals(Accessories.MODID)) return SlotAttribute.getAttributeHolder(attributeType.getPath());

					return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
							.registryManager()
							.get(RegistryKeys.ATTRIBUTE)
							.getEntry(attributeType)
							.orElseThrow(IllegalStateException::new);
				},
				(context, attributeHolder) -> {
					var attribute = attributeHolder.value();

					if(attribute instanceof SlotAttribute slotAttribute) return Accessories.of(slotAttribute.slotName());

					return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
							.registryManager()
							.get(RegistryKeys.ATTRIBUTE)
							.getId(attribute);
				}
		);

		public static final Endec<Entry> ENDEC = StructEndecBuilder.of(
				ATTRIBUTE_ENDEC.fieldOf("type", Entry::attribute),
				AttributeUtils.ATTRIBUTE_MODIFIER_ENDEC.flatFieldOf(Entry::modifier),
				Endec.STRING.optionalOf().fieldOf("slot", Entry::slot),
				Entry::new
		);

		public static final Codec<Entry> CODEC = CodecUtils.toCodec(ENDEC);
		public static final PacketCodec<RegistryByteBuf, Entry> PACKET_CODEC = CodecUtils.toPacketCodec(ENDEC);
	}
}