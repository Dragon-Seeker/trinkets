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

import dev.emi.trinkets.api.TrinketEnums.DropRule;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SlotType {

	private final String group;
	private final String name;
	private final int order;
	private final int amount;
	private final Identifier icon;
	private final Set<Identifier> quickMovePredicates;
	private final Set<Identifier> validatorPredicates;
	private final Set<Identifier> tooltipPredicates;
	private final DropRule dropRule;

	public SlotType(String group, String name, int order, int amount, Identifier icon, Set<Identifier> quickMovePredicates,
			Set<Identifier> validatorPredicates, Set<Identifier> tooltipPredicates, DropRule dropRule) {
		this.group = group;
		this.name = name;
		this.order = order;
		this.amount = amount;
		this.icon = icon;
		this.quickMovePredicates = quickMovePredicates;
		this.validatorPredicates = validatorPredicates;
		this.tooltipPredicates = tooltipPredicates;
		this.dropRule = dropRule;
	}

	public String getGroup() {
		return group;
	}

	public String getName() {
		return name;
	}

	public int getOrder() {
		return order;
	}

	public int getAmount() {
		return amount;
	}

	public Identifier getIcon() {
		return icon;
	}

	public Set<Identifier> getQuickMovePredicates() {
		return quickMovePredicates;
	}

	public Set<Identifier> getValidatorPredicates() {
		return validatorPredicates;
	}

	public Set<Identifier> getTooltipPredicates() {
		return tooltipPredicates;
	}

	public DropRule getDropRule() {
		return dropRule;
	}

	public MutableText getTranslation() {
		return Text.translatable("trinkets.slot." + this.group + "." + this.name);
	}

	public void write(NbtCompound data) {
		NbtCompound tag = new NbtCompound();
		tag.putString("Group", group);
		tag.putString("Name", name);
		tag.putInt("Order", order);
		tag.putInt("Amount", amount);
		tag.putString("Icon", icon.toString());
		NbtList quickMovePredicateList = new NbtList();

		for (Identifier id : quickMovePredicates) {
			quickMovePredicateList.add(NbtString.of(id.toString()));
		}
		tag.put("QuickMovePredicates", quickMovePredicateList);

		NbtList validatorPredicateList = new NbtList();

		for (Identifier id : validatorPredicates) {
			validatorPredicateList.add(NbtString.of(id.toString()));
		}
		tag.put("ValidatorPredicates", validatorPredicateList);

		NbtList tooltipPredicateList = new NbtList();

		for (Identifier id : tooltipPredicates) {
			tooltipPredicateList.add(NbtString.of(id.toString()));
		}
		tag.put("TooltipPredicates", tooltipPredicateList);
		tag.putString("DropRule", dropRule.toString());
		data.put("SlotData", tag);
	}

	public static SlotType read(NbtCompound data) {
		NbtCompound slotData = data.getCompound("SlotData");
		String group = slotData.getString("Group");
		String name = slotData.getString("Name");
		int order = slotData.getInt("Order");
		int amount = slotData.getInt("Amount");
		Identifier icon = Identifier.of(slotData.getString("Icon"));
		NbtList quickMoveList = slotData.getList("QuickMovePredicates", NbtElement.STRING_TYPE);
		Set<Identifier> quickMovePredicates = new HashSet<>();

		for (NbtElement tag : quickMoveList) {
			quickMovePredicates.add(Identifier.of(tag.asString()));
		}
		NbtList validatorList = slotData.getList("ValidatorPredicates", NbtElement.STRING_TYPE);
		Set<Identifier> validatorPredicates = new HashSet<>();

		for (NbtElement tag : validatorList) {
			validatorPredicates.add(Identifier.of(tag.asString()));
		}
		NbtList tooltipList = slotData.getList("TooltipPredicates", NbtElement.STRING_TYPE);
		Set<Identifier> tooltipPredicates = new HashSet<>();

		for (NbtElement tag : tooltipList) {
			tooltipPredicates.add(Identifier.of(tag.asString()));
		}
		String dropRuleName = slotData.getString("DropRule");
		DropRule dropRule = DropRule.DEFAULT;

		if (TrinketEnums.DropRule.has(dropRuleName)) {
			dropRule = TrinketEnums.DropRule.valueOf(dropRuleName);
		}
		return new SlotType(group, name, order, amount, icon, quickMovePredicates, validatorPredicates, tooltipPredicates, dropRule);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		SlotType slotType = (SlotType) o;
		return group.equals(slotType.group) && name.equals(slotType.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(group, name);
	}

	public String getId() {
		return this.group + "/" + this.name;
	}
}
