package io.wispforest.tclayer;

import io.wispforest.tclayer.compat.config.TCLayerConfig;
import net.fabricmc.api.ModInitializer;

public class TCLayer implements ModInitializer {

    public final static TCLayerConfig CONFIG = TCLayerConfig.createAndLoad();

    @Override
    public void onInitialize() {}
}
