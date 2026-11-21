package com.galacticpurpose.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Recoverable expedition black-box log recording the fate of earlier Erebus scout missions.
 */
public class ExpeditionLogItem extends Item {
	private final int logNumber;

	public ExpeditionLogItem(int logNumber, Properties properties) {
		super(properties);
		this.logNumber = logNumber;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide) {
			level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
			String base = "lore.galacticpurpose.log" + this.logNumber;
			player.displayClientMessage(Component.translatable(base + ".title").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
			player.displayClientMessage(Component.translatable(base + ".line1").withStyle(ChatFormatting.GRAY), false);
			player.displayClientMessage(Component.translatable(base + ".line2").withStyle(ChatFormatting.AQUA), false);
		}
		return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.translatable("tooltip.galacticpurpose.expedition_log_hint").withStyle(ChatFormatting.DARK_AQUA));
	}
}
