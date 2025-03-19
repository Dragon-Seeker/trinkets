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

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.string;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

import dev.emi.trinkets.api.*;
import dev.emi.trinkets.payload.BreakPayload;
import dev.emi.trinkets.payload.SyncInventoryPayload;
import dev.emi.trinkets.payload.SyncSlotsPayload;
import io.wispforest.tclayer.compat.WrappedTrinketComponent;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;

import dev.emi.trinkets.data.EntitySlotLoader;
import dev.emi.trinkets.data.SlotLoader;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;
import org.ladysnake.cca.api.v3.entity.RespawnCopyStrategy;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.injection.struct.InjectorGroupInfo;

import java.util.Map;

public class TrinketsMain implements ModInitializer, EntityComponentInitializer {

	public static final String MOD_ID = "trinkets";
	public static final Logger LOGGER = LogManager.getLogger();

	@Override
	public void onInitialize() {
//		ResourceManagerHelper resourceManagerHelper = ResourceManagerHelper.get(ResourceType.SERVER_DATA);
//		resourceManagerHelper.registerReloadListener(SlotLoader.INSTANCE);
//		resourceManagerHelper.registerReloadListener(EntitySlotLoader.SERVER);
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success)
				-> EntitySlotLoader.SERVER.sync(server.getPlayerManager().getPlayerList()));
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			Trinket trinket = TrinketsApi.getTrinket(stack.getItem());
			if (trinket.canEquipFromUse(stack, player)) {
				if (TrinketItem.equipItem(player, stack)) {
					return TypedActionResult.success(stack);
				}
			}
			return TypedActionResult.pass(stack);
		});
		Registry.register(Registries.DATA_COMPONENT_TYPE, Identifier.of(MOD_ID, "attribute_modifiers"), TrinketsAttributeModifiersComponent.TYPE);
		PayloadTypeRegistry.playS2C().register(TrinketsNetwork.BREAK, BreakPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TrinketsNetwork.SYNC_INVENTORY, SyncInventoryPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(TrinketsNetwork.SYNC_SLOTS, SyncSlotsPayload.CODEC);
		CommandRegistrationCallback.EVENT.register((dispatcher, registry, env) ->
			dispatcher.register(literal("trinkets")
				.requires(source -> source.hasPermissionLevel(2))
				.then(
					literal("set")
					.then(
						argument("group", string())
						.then(
							argument("slot", string())
							.then(
								argument("offset", integer(0))
								.then(
									argument("stack", ItemStackArgumentType.itemStack(registry))
									.executes(context -> {
										try {
										return trinketsCommand(context, 1);

										} catch (Exception e) {
											e.printStackTrace();
											return -1;
										}
									})
									.then(
										argument("count", integer(1))
										.executes(context -> {
											int amount = context.getArgument("amount", Integer.class);
											return trinketsCommand(context, amount);
										})
									)
								)
							)
						)
					)
				)
				.then(
					literal("clear")
					.executes(context -> {
						try {
							return clearCommand(context);
						} catch (Exception e){
							e.printStackTrace();
							return -1;
						}
					})
				)
			));
	}

	private static int clearCommand(CommandContext<ServerCommandSource> context){
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player != null) {
			TrinketComponent comp = TrinketsApi.getTrinketComponent(player).get();
			for (Map.Entry<String, Map<String, TrinketInventory>> entry : comp.getInventory().entrySet()){
				for (TrinketInventory inv : entry.getValue().values()){
					inv.clear();
				}
			}
		}
		return 1;
	}

	private static int trinketsCommand(CommandContext<ServerCommandSource> context, int amount) {
		try {
			String group = context.getArgument("group", String.class);
			String slot = context.getArgument("slot", String.class);
			int offset = context.getArgument("offset", Integer.class);
			ItemStackArgument stack = context.getArgument("stack", ItemStackArgument.class);
			ServerPlayerEntity player = context.getSource().getPlayer();
			if (player != null) {
				TrinketComponent comp = TrinketsApi.getTrinketComponent(player).get();
				SlotGroup slotGroup = comp.getGroups().getOrDefault(group, null);
				if (slotGroup != null) {
					SlotType slotType = slotGroup.getSlots().getOrDefault(slot, null);
					if (slotType != null) {
						if (offset >= 0 && offset < slotType.getAmount()) {
							comp.getInventory().get(group).get(slot).setStack(offset, stack.createStack(amount, true));
							return Command.SINGLE_SUCCESS;
						} else {
							context.getSource().sendError(Text.literal(offset + " offset does not exist for slot"));
						}
					} else {
						context.getSource().sendError(Text.literal(slot + " does not exist"));
					}
				} else {
					context.getSource().sendError(Text.literal(group + " does not exist"));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerFor(LivingEntity.class, TrinketsApi.TRINKET_COMPONENT, WrappedTrinketComponent::new);
		registry.registerForPlayers(TrinketsApi.TRINKET_COMPONENT, WrappedTrinketComponent::new, RespawnCopyStrategy.ALWAYS_COPY);
	}
}