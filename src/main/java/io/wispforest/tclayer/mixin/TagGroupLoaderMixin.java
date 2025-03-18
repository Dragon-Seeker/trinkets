package io.wispforest.tclayer.mixin;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.logging.LogUtils;
import io.wispforest.tclayer.TCLayer;
import io.wispforest.tclayer.compat.WrappingTrinketsUtils;
import io.wispforest.tclayer.compat.config.SlotIdRedirect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagGroupLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.BiConsumer;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This mixin acts as a method to adjust tags for [Curios -> Trinkets <-> Accessories] specially
 * with another mixin added to handle [Trinkets -> Curios <-> Accessories] within the CCLayer.
 */
@Mixin(value = TagGroupLoader.class, priority = 2000)
public abstract class TagGroupLoaderMixin {

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Final
    @Shadow
    private String dataType;

    @Inject(method = "loadTags", at = @At("TAIL"), order = 900)
    public void setupShares(ResourceManager resourceManager, CallbackInfoReturnable<Map<Identifier, List<TagGroupLoader.TrackedEntry>>> cir,
                            @Share(namespace = "accessories", value = "curiosToAccessoryCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> curiosToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToCuriosCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> accessoryToCuriosCalls_share,
                            @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> trinketToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> accessoryToTrinketCalls_share) {
        curiosToAccessoryCalls_share.set(new HashMap<>());
        accessoryToCuriosCalls_share.set(new HashMap<>());
        trinketToAccessoryCalls_share.set(new HashMap<>());
        accessoryToTrinketCalls_share.set(new HashMap<>());
    }

    @Inject(method = "loadTags", at = @At("TAIL"))
    public void injectValues(ResourceManager resourceManager, CallbackInfoReturnable<Map<Identifier, List<TagGroupLoader.TrackedEntry>>> cir,
                             @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> trinketToAccessoryCalls_share,
                             @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> accessoryToTrinketCalls_share) {
        if (!RegistryKeys.getTagPath(Registries.ITEM.getKey()).equals(dataType)) return;

        var map = cir.getReturnValue();

        var redirects = SlotIdRedirect.getBiMap(TCLayer.CONFIG.slotIdRedirects());

        var trinketToAccessoryCalls = trinketToAccessoryCalls_share.get();
        var accessoryToTrinketCalls = accessoryToTrinketCalls_share.get();

        TriConsumer<Identifier, Identifier, List<TagGroupLoader.TrackedEntry>> addCallback = (fromLocation, toLocation, tagEntries) -> {
            LOGGER.warn("Adding Entries from [{}] to [{}]: \n     {}", fromLocation, toLocation, tagEntries);
            map.computeIfAbsent(toLocation, location1 -> new ArrayList<>()).addAll(tagEntries);
        };

        Map.copyOf(map).forEach((location, entries) -> {
            var entriesCopy = new ArrayList<>(entries);

            if(location.getNamespace().equals("trinkets")) {
                var path = location.getPath();

                var parts = path.split("/");

                if (parts.length != 2) return;

                var group = parts[0];
                var slot = parts[1];

                WrappingTrinketsUtils.trinketsToAccessories_SlotEither(Optional.of(group), slot)
                        .ifRight(slotName -> {
                            var accessoryTag = Identifier.of("accessories", slotName);

                            trinketToAccessoryCalls.put(accessoryTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));
                        }).ifLeft(slotName -> {
                            var accessoryTag = Identifier.of("accessories", slotName);

                            trinketToAccessoryCalls.put(accessoryTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));

                            var redirect = redirects.get(location.getPath());

                            if (redirect != null) {
                                var accessoryRedirectTag = Identifier.of("accessories", redirect);

                                trinketToAccessoryCalls.put(accessoryRedirectTag, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, accessoryTag, tagEntries)));
                            }
                        });
            } else if(location.getNamespace().equals("accessories")) {
                var possibleGroups = WrappingTrinketsUtils.getGroupFromDefaultSlot(location.getPath());

                if (!possibleGroups.isEmpty()) {
                    for (var group : possibleGroups) {
                        var trinketTag = Identifier.of("trinkets", group + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(location.getPath()));

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, trinketTag, tagEntries)));
                    }
                } else {
                    var groupInfo = WrappingTrinketsUtils.getGroupInfo(location.getPath());

                    if (groupInfo != null) {
                        var trinketTag = Identifier.of("trinkets", (groupInfo + "/" + WrappingTrinketsUtils.accessoriesToTrinkets_Slot(location.getPath())));

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, trinketTag, tagEntries)));
                    }

                    var redirect = redirects.inverse().get(location.getPath());

                    if (redirect != null) {
                        var redirectTag = Identifier.of("trinkets", redirect);

                        accessoryToTrinketCalls.put(location, Triple.of(location, entriesCopy, (fromLocation, tagEntries) -> addCallback.accept(fromLocation, redirectTag, tagEntries)));
                    }
                }
            }
        });
    }

    @Inject(method = "loadTags", at = @At("TAIL"), order = 1100)
    public void handleShares(ResourceManager resourceManager, CallbackInfoReturnable<Map<Identifier, List<TagGroupLoader.TrackedEntry>>> cir,
                            @Share(namespace = "accessories", value = "curiosToAccessoryCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> curiosToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToCuriosCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> accessoryToCuriosCalls_share,
                            @Share(namespace = "accessories", value = "trinketToAccessoryCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> trinketToAccessoryCalls_share,
                            @Share(namespace = "accessories", value = "accessoryToTrinketCalls") LocalRef<Map<Identifier, Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>>>> accessoryToTrinketCalls_share) {
        if (!RegistryKeys.getTagPath(Registries.ITEM.getKey()).equals(dataType)) return;

        var curiosToAccessoryCalls = curiosToAccessoryCalls_share.get();
        var trinketToAccessoryCalls = trinketToAccessoryCalls_share.get();

        var accessoryToCuriosCalls = accessoryToCuriosCalls_share.get();
        var accessoryToTrinketCalls = accessoryToTrinketCalls_share.get();

        var allLocations = Stream.of(accessoryToCuriosCalls.keySet(), curiosToAccessoryCalls.keySet(), accessoryToTrinketCalls.keySet(), trinketToAccessoryCalls.keySet())
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());

        for (var accessoryLocation : allLocations) {
            var trinketEntries = trinketToAccessoryCalls.get(accessoryLocation);
            var curiosEntries = curiosToAccessoryCalls.get(accessoryLocation);

            if (trinketEntries != null) {
                accessoryToCuriosCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE).getRight().accept(trinketEntries.getLeft(), trinketEntries.getMiddle());
                curiosToAccessoryCalls.getOrDefault(accessoryLocation, trinketToAccessoryCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE)).getRight().accept(trinketEntries.getLeft(), trinketEntries.getMiddle());
            }

            if (curiosEntries != null) {
                accessoryToTrinketCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE).getRight().accept(curiosEntries.getLeft(), curiosEntries.getMiddle());
                curiosToAccessoryCalls.getOrDefault(accessoryLocation, trinketToAccessoryCalls.getOrDefault(accessoryLocation, EMPTY_TRIPLE)).getRight().accept(curiosEntries.getLeft(), curiosEntries.getMiddle());
            }

            Optional.ofNullable(accessoryToCuriosCalls.get(accessoryLocation)).ifPresent(triple -> triple.getRight().accept(triple.getLeft(), triple.getMiddle()));
            Optional.ofNullable(accessoryToTrinketCalls.get(accessoryLocation)).ifPresent(triple -> triple.getRight().accept(triple.getLeft(), triple.getMiddle()));
        }

        curiosToAccessoryCalls_share.set(new HashMap<>());
        accessoryToCuriosCalls_share.set(new HashMap<>());
        trinketToAccessoryCalls_share.set(new HashMap<>());
        accessoryToTrinketCalls_share.set(new HashMap<>());
    }

    @Unique
    private static final Identifier INVALID_ID = Identifier.of("invalid", "none");

    @Unique
    private static final Triple<Identifier, List<TagGroupLoader.TrackedEntry>, BiConsumer<Identifier, List<TagGroupLoader.TrackedEntry>>> EMPTY_TRIPLE = Triple.of(INVALID_ID, List.of(), (fromLocation, entryWithSources) -> {});
}
