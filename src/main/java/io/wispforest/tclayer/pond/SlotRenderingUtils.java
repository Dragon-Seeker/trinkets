package io.wispforest.tclayer.pond;

import com.mojang.datafixers.util.Pair;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.owo.ui.core.OwoUIDrawContext;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.MissingSprite;
import net.minecraft.client.texture.Sprite;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;

public class SlotRenderingUtils {

    public static boolean renderSlotTexture(DrawContext instance, int x, int y, int blitOffset, int width, int height, Sprite sprite, Slot slot, Pair<Identifier, Identifier> slotTexture) {
        if(slot instanceof AccessoriesBasedSlot && sprite.getContents().getId().equals(MissingSprite.getMissingSpriteId())) {
            var location = Identifier.of(slotTexture.getSecond().getNamespace(), "textures/" + slotTexture.getSecond().getPath() + ".png");

            // Little bug with Accessories stupid batching...
            if (instance instanceof OwoUIDrawContext context && context.recording()) {
                try {
                    context.submitQuads();
                } catch (Exception ignored) {}
            }

            instance.drawTexture(location, x, y, blitOffset, 0, 0, width, height, width, height);

            return true;
        }

        return false;
    }
}
