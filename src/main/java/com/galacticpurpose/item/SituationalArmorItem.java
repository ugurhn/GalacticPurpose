package com.galacticpurpose.item;

import com.galacticpurpose.GalacticPurpose;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

/**
 * Shared base for situational Galactic Purpose protective suits with Anti-God-Mode speed trade-offs.
 */
public abstract class SituationalArmorItem extends ArmorItem {
	private final double speedPenaltyPerPiece;
	private final String modifierPrefix;
	private final String tooltipKey;
	private final ChatFormatting tooltipColor;
	private ItemAttributeModifiers cachedModifiers;

	protected SituationalArmorItem(
		Holder<ArmorMaterial> material,
		Type type,
		Properties properties,
		double speedPenaltyPerPiece,
		String modifierPrefix,
		String tooltipKey,
		ChatFormatting tooltipColor
	) {
		super(material, type, properties);
		this.speedPenaltyPerPiece = speedPenaltyPerPiece;
		this.modifierPrefix = modifierPrefix;
		this.tooltipKey = tooltipKey;
		this.tooltipColor = tooltipColor;
	}

	@Override
	public ItemAttributeModifiers getDefaultAttributeModifiers() {
		if (this.cachedModifiers == null) {
			ItemAttributeModifiers base = super.getDefaultAttributeModifiers();
			if (this.speedPenaltyPerPiece != 0.0) {
				base = base.withModifierAdded(
					Attributes.MOVEMENT_SPEED,
					new AttributeModifier(
						GalacticPurpose.id(this.modifierPrefix + this.type.getName()),
						this.speedPenaltyPerPiece,
						AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
					),
					EquipmentSlotGroup.bySlot(this.type.getSlot())
				);
			}
			this.cachedModifiers = base;
		}
		return this.cachedModifiers;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable(this.tooltipKey).withStyle(this.tooltipColor));
	}

	protected static <T extends SituationalArmorItem> boolean hasFullSetOf(LivingEntity entity, Class<T> armorClass) {
		return armorClass.isInstance(entity.getItemBySlot(EquipmentSlot.HEAD).getItem())
			&& armorClass.isInstance(entity.getItemBySlot(EquipmentSlot.CHEST).getItem())
			&& armorClass.isInstance(entity.getItemBySlot(EquipmentSlot.LEGS).getItem())
			&& armorClass.isInstance(entity.getItemBySlot(EquipmentSlot.FEET).getItem());
	}
}
