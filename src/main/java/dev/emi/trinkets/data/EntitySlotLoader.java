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
package dev.emi.trinkets.data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import dev.emi.trinkets.TrinketPlayerScreenHandler;
import dev.emi.trinkets.TrinketsMain;
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.data.SlotLoader.GroupData;
import dev.emi.trinkets.data.SlotLoader.SlotData;
import dev.emi.trinkets.payload.SyncSlotsPayload;
import io.wispforest.tclayer.ImmutableDelegatingMap;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceReloadListenerKeys;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SinglePreparationResourceReloader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.profiler.Profiler;

public class EntitySlotLoader extends SinglePreparationResourceReloader<Map<String, Map<String, Set<String>>>> implements IdentifiableResourceReloadListener {

	public static final EntitySlotLoader CLIENT = new EntitySlotLoader(true);
	public static final EntitySlotLoader SERVER = new EntitySlotLoader(false);

	private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().disableHtmlEscaping().create();
	private static final Identifier ID = Identifier.of(TrinketsMain.MOD_ID, "entities");

	private final Map<EntityType<?>, Map<String, SlotGroup>> slots = new HashMap<>();

	private boolean isClient = false;

	public EntitySlotLoader(boolean isClient) {
		this();

		this.isClient = isClient;
	}

	public EntitySlotLoader() {
		super();
	}

	@Override
	protected Map<String, Map<String, Set<String>>> prepare(ResourceManager resourceManager, Profiler profiler) {
		Map<String, Map<String, Set<String>>> map = new HashMap<>();
		String dataType = "entities";

		for (Map.Entry<Identifier, List<Resource>> entry : resourceManager.findAllResources(dataType, id -> id.getPath().endsWith(".json")).entrySet()) {
			Identifier identifier = entry.getKey();

			if (identifier.getNamespace().equals(TrinketsMain.MOD_ID)) {

				try {
					for (Resource resource : entry.getValue()) {
						InputStreamReader reader = new InputStreamReader(resource.getInputStream());
						JsonObject jsonObject = JsonHelper.deserialize(GSON, reader, JsonObject.class);

						if (jsonObject != null) {

							try {
								boolean replace = JsonHelper.getBoolean(jsonObject, "replace", false);
								JsonArray assignedSlots = JsonHelper.getArray(jsonObject, "slots", new JsonArray());
								Map<String, Set<String>> groups = new HashMap<>();

								if (assignedSlots != null) {

									for (JsonElement assignedSlot : assignedSlots) {
										String slot = assignedSlot.getAsString();
										String[] parsedSlot = slot.split("/");

										if (parsedSlot.length != 2) {
											TrinketsMain.LOGGER.error("Detected malformed slot assignment " + slot
													+ "! Slots should be in the format 'group/slot'.");
											continue;
										}
										String group = parsedSlot[0];
										String name = parsedSlot[1];
										groups.computeIfAbsent(group, (k) -> new HashSet<>()).add(name);
									}
								}
								JsonArray entities = JsonHelper.getArray(jsonObject, "entities", new JsonArray());

								if (!groups.isEmpty() && entities != null) {

									for (JsonElement entity : entities) {
										String name = entity.getAsString();
										String id;

										if (name.startsWith("#")) {
											id = "#" + Identifier.of(name.substring(1));
										} else {
											id = Identifier.of(name).toString();
										}
										Map<String, Set<String>> slots = map.computeIfAbsent(id, (k) -> new HashMap<>());

										if (replace) {
											slots.clear();
										}
										groups.forEach((groupName, slotNames) -> slots.computeIfAbsent(groupName, (k) -> new HashSet<>())
												.addAll(slotNames));
									}
								}
							} catch (JsonSyntaxException e) {
								TrinketsMain.LOGGER.error("[trinkets] Syntax error while reading data for " + identifier.getPath());
								e.printStackTrace();
							}
						}

					}
				} catch (IOException e) {
					TrinketsMain.LOGGER.error("[trinkets] Unknown IO error while reading slot data!");
					e.printStackTrace();
				}
			}
		}
		return map;
	}

	public final Map<EntityType<?>, Map<String, Set<String>>> slotInfo = new HashMap<>();

	@Override
	protected void apply(Map<String, Map<String, Set<String>>> loader, ResourceManager manager, Profiler profiler) {
		slotInfo.clear();

		Map<String, GroupData> slots = SlotLoader.INSTANCE.getSlots();
		Map<EntityType<?>, Map<String, SlotGroup.Builder>> groupBuilders = new HashMap<>();

		loader.forEach((entityName, groups) -> {
			Set<EntityType<?>> types = new HashSet<>();

			try {
				if (entityName.startsWith("#")) {
					// TODO rewrite this to work with the new tag system
					TrinketsMain.LOGGER.error("[trinkets] Attempted to assign entity entry to tag");
					/*
					TagKey<EntityType<?>> tag = TagKey.of(Registry.ENTITY_TYPE_KEY, Identifier.of(entityName.substring(1)));
					List<? extends EntityType<?>> entityTypes = Registry.ENTITY_TYPE.getEntryList(tag)
							.orElseThrow(() -> new IllegalArgumentException("Unknown entity tag '" + entityName + "'"))
							.stream()
							.map(RegistryEntry::value)
							.toList();

					types.addAll(entityTypes);*/
				} else {
					types.add(Registries.ENTITY_TYPE.getOrEmpty(Identifier.of(entityName))
							.orElseThrow(() -> new IllegalArgumentException("Unknown entity '" + entityName + "'")));
				}
			} catch (IllegalArgumentException e) {
				TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown entity entry " + entityName);
			}

			for (EntityType<?> type : types) {
				slotInfo.put(type, ImmutableMap.copyOf(groups));
				// Keep code there but just don't run it
				if (true) continue;

				Map<String, SlotGroup.Builder> builders = groupBuilders.computeIfAbsent(type, (k) -> new HashMap<>());
				groups.forEach((groupName, slotNames) -> {
					GroupData group = slots.get(groupName);

					if (group != null) {
						SlotGroup.Builder builder = builders.computeIfAbsent(groupName,
								(k) -> new SlotGroup.Builder(groupName, group.getSlotId(), group.getOrder()));
						slotNames.forEach(slotName -> {
							SlotData slotData = group.getSlot(slotName);

							if (slotData != null) {
								builder.addSlot(slotName, slotData.create(groupName, slotName));
							} else {
								TrinketsMain.LOGGER.error("[trinkets] Attempted to assign unknown slot " + slotName);
							}
						});
					} else {
						TrinketsMain.LOGGER.error("[trinkets] Attempted to assign slot from unknown group " + groupName);
					}
				});
			}
		});
		this.slots.clear();

		if (true) return;
		groupBuilders.forEach((entity, groups) -> {
			Map<String, SlotGroup> entitySlots = this.slots.computeIfAbsent(entity, (k) -> new HashMap<>());
			groups.forEach((groupName, groupBuilder) -> entitySlots.putIfAbsent(groupName, groupBuilder.build()));
		});
	}

	public Map<String, SlotGroup> getEntitySlots(EntityType<?> entityType) {
		return ImmutableDelegatingMap.slotGroups(WrappingTrinketsUtils.getGroupedSlots(this.isClient, entityType), this.isClient);
	}

	public void setSlots(Map<EntityType<?>, Map<String, SlotGroup>> slots) {
		this.slots.clear();
		this.slots.putAll(slots);
	}

	public void sync(ServerPlayerEntity playerEntity) {
		if (true) return;
		ServerPlayNetworking.send(playerEntity, new SyncSlotsPayload(Map.copyOf(this.slots)));
	}

	public void sync(List<? extends ServerPlayerEntity> players) {
		if (true) return;
		SyncSlotsPayload packet = new SyncSlotsPayload(Map.copyOf(this.slots));
		players.forEach(player -> ServerPlayNetworking.send(player, packet));
		players.forEach(player -> ((TrinketPlayerScreenHandler) player.playerScreenHandler).trinkets$updateTrinketSlots(true));
	}

	@Override
	public Identifier getFabricId() {
		return ID;
	}

	@Override
	public Collection<Identifier> getFabricDependencies() {
		return Lists.newArrayList(SlotLoader.ID, ResourceReloadListenerKeys.TAGS);
	}
}