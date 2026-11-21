package com.galacticpurpose.client.screen;

import com.galacticpurpose.menu.BioDistillerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * UI screen for the Exotic Bio-Fuel Distiller.
 */
public class BioDistillerScreen extends AbstractContainerScreen<BioDistillerMenu> {
	public BioDistillerScreen(BioDistillerMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 186;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 93;
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;

		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A202C);
		graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF4A5568);
		graphics.fill(x + 4, y + 18, x + this.imageWidth - 4, y + 89, 0xFF11161F);

		// Battery (0: 12,46), Input Bio-Essence (1: 56,46), Output Bio-Fuel (2: 132,46)
		drawSlotBox(graphics, x + 11, y + 45, 0xFF00D4FF);
		drawSlotBox(graphics, x + 55, y + 45, 0xFF22C55E);
		drawSlotBox(graphics, x + 131, y + 45, 0xFFC026D3);

		// Vertical Energy bar (x + 34, y + 23, width 10, height 60)
		int barX = x + 34;
		int barY = y + 23;
		int barW = 10;
		int barH = 60;
		graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0D12);
		graphics.renderOutline(barX, barY, barW, barH, 0xFF334155);

		float energyRatio = clamp01(this.menu.energyStored() / (float) this.menu.energyCapacity());
		int filledH = Math.round((barH - 2) * energyRatio);
		if (filledH > 0) {
			graphics.fill(barX + 1, barY + barH - 1 - filledH, barX + barW - 1, barY + barH - 1, 0xFF00D4FF);
		}

		// Distillation progress bar between input & output slots
		float progressRatio = clamp01(this.menu.progress() / (float) this.menu.maxProgress());
		drawProgressBar(graphics, x + 78, y + 48, 48, 12, progressRatio, 0xFFC026D3);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlotBox(graphics, x + 7 + col * 18, y + 103 + row * 18, 0xFF2D3748);
			}
		}
		for (int col = 0; col < 9; col++) {
			drawSlotBox(graphics, x + 7 + col * 18, y + 161, 0xFF2D3748);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xF1F5F9, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCBD5E1, false);

		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.distiller_recipe"),
			54,
			25,
			0xE2E8F0,
			false
		);

		int pct = Math.round(clamp01(this.menu.progress() / (float) this.menu.maxProgress()) * 100.0f);
		String pctText = pct + "%";
		int textW = this.font.width(pctText);
		graphics.drawString(this.font, pctText, 78 + (48 - textW) / 2, 50, 0xFFFFFF, true);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int barX = this.leftPos + 34;
		int barY = this.topPos + 23;
		if (mouseX >= barX && mouseX <= barX + 10 && mouseY >= barY && mouseY <= barY + 60) {
			graphics.renderTooltip(
				this.font,
				Component.translatable(
					"gui.galacticpurpose.energy_tooltip",
					this.menu.energyStored(),
					this.menu.energyCapacity()
				),
				mouseX,
				mouseY
			);
		} else {
			this.renderTooltip(graphics, mouseX, mouseY);
		}
	}

	private static float clamp01(float value) {
		return Math.min(1.0f, Math.max(0.0f, value));
	}

	private static void drawSlotBox(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x, y, x + 18, y + 18, borderColor);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F172A);
	}

	private static void drawProgressBar(
		GuiGraphics graphics,
		int x,
		int y,
		int width,
		int height,
		float ratio,
		int fillColor
	) {
		graphics.fill(x, y, x + width, y + height, 0xFF0A0D12);
		graphics.renderOutline(x, y, width, height, 0xFF334155);
		int filled = Math.round((width - 2) * ratio);
		if (filled > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, fillColor);
		}
	}
}
