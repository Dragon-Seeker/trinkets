package io.wispforest.tclayer.compat;

import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketEnums;
import io.wispforest.accessories.data.SlotGroupLoader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class WrappedSlotType extends SlotType {

    public final io.wispforest.accessories.api.slot.SlotType slotType;

    @Nullable
    private String otherGroup = null;

    public WrappedSlotType(io.wispforest.accessories.api.slot.SlotType slotType, String group){
        super(group, "", 0, 0, Identifier.of(""), Set.of(), Set.of(), Set.of(), null);

        this.slotType = slotType;
    }

    public static WrappedSlotType of(io.wispforest.accessories.api.slot.SlotType slotType, boolean isClientSide){
        var groups = SlotGroupLoader.INSTANCE.getGroups(isClientSide, false);

        var slotGroup = "";

        for (var group : groups) {
            if(group.slots().contains(slotType.name())) {
                slotGroup = WrappingTrinketsUtils.accessoriesToTrinkets_Group(group.name());

                break;
            }
        }

        return new WrappedSlotType(slotType, slotGroup);
    }


    public void setOtherGroupName(String value) {
        otherGroup = value;
    }

    @Override
    public String getName() {
        return WrappingTrinketsUtils.accessoriesToTrinkets_Slot(slotType.name());
    }

    @Override
    public String getGroup() {
        return (otherGroup != null) ? otherGroup : super.getGroup();
    }

    @Override
    public int getOrder() {
        return slotType.order();
    }

    @Override
    public int getAmount() {
        return slotType.amount();
    }

    @Override
    public Identifier getIcon() {
        return slotType.icon();
    }

    @Override
    public TrinketEnums.DropRule getDropRule() {
        return WrappingTrinketsUtils.convertDropRule(slotType.dropRule());
    }

    public MutableText getTranslation() {
        return Text.translatable("trinkets.slot." + this.getGroup() + "." + this.getName());
    }

    @Override
    public Set<Identifier> getQuickMovePredicates() {
        return this.slotType.validators();
    }

    @Override
    public Set<Identifier> getValidatorPredicates() {
        return this.slotType.validators();
    }

    @Override
    public Set<Identifier> getTooltipPredicates() {
        return this.slotType.validators();
    }

    @Override
    public int hashCode() {
        return this.slotType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof WrappedSlotType wrappedSlotType) {
            obj = wrappedSlotType.slotType;
        }

        return this.slotType.equals(obj);
    }
}
