package com.galacticpurpose.client.screen;

import com.galacticpurpose.entity.InterstellarShipEntity;
import com.galacticpurpose.menu.StarshipMenu;
import com.galacticpurpose.wormhole.WormholeLinkCalculator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Sci-fi Starship Cargo Bay, Refuel/Energy Console & Orbital Launch Control Screen.
 */
public class StarshipScreen extends AbstractContainerScreen<StarshipMenu> {
	public StarshipScreen(StarshipMenu menu, Inventory inv, Component title) {
		super(menu, inv, title);
		this.imageWidth = 176;
		this.imageHeight = 196;
		this.titleLabelX = 8;
		this.titleLabelY = 6;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 103;
	}

	@Override
	protected void init() {
		super.init();
		this.addRenderableWidget(Button.builder(
			Component.translatable("gui.galacticpurpose.ship_btn_launch"),
			b -> pressButton(0)
		).bounds(this.leftPos + 50, this.topPos + 19, 64, 18).build());
		this.addRenderableWidget(Button.builder(
			Component.translatable("gui.galacticpurpose.ship_btn_pack"),
			b -> pressButton(1)
		).bounds(this.leftPos + 116, this.topPos + 19, 52, 18).build());
	}

	private void pressButton(int id) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
			this.onClose();
		}
	}

	@Override
	protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos, y = this.topPos;
		g.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF0E1524);
		g.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF38BDF8);

		// Fuel & Energy intake slot frames
		drawSlot(g, x + 7, y + 19, 0xFFF97316);
		drawSlot(g, x + 27, y + 19, 0xFF00D4FF);

		// 27 Cargo slots
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				drawSlot(g, x + 7 + c * 18, y + 45 + r * 18, 0xFF334155);
			}
		}
		// Player inventory
		for (int r = 0; r < 3; r++) {
			for (int c = 0; c < 9; c++) {
				drawSlot(g, x + 7 + c * 18, y + 113 + r * 18, 0xFF1E293B);
			}
		}
		for (int c = 0; c < 9; c++) {
			drawSlot(g, x + 7 + c * 18, y + 171, 0xFF1E293B);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
		g.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFFFACC15, false);
		g.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFF94A3B8, false);

		String fuelTxt = "Fuel " + this.menu.fuelUnits() + "/" + InterstellarShipEntity.MAX_FUEL
			+ " | " + this.menu.energyKilo() + "kE";
		g.drawString(this.font, fuelTxt, 8, 38, 0xFF7DD3FC, false);

		WormholeLinkCalculator.LinkStatus[] vals = WormholeLinkCalculator.LinkStatus.values();
		int ord = this.menu.linkStatusOrdinal();
		WormholeLinkCalculator.LinkStatus st = (ord >= 0 && ord < vals.length) ? vals[ord] : WormholeLinkCalculator.LinkStatus.UNLINKED;
		int col = st == WormholeLinkCalculator.LinkStatus.OPEN ? 0xFF22C55E : 0xFFEF4444;
		g.drawString(this.font, st.name(), 118, 6, col, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		super.render(g, mouseX, mouseY, partialTick);
		this.renderTooltip(g, mouseX, mouseY);
	}

	private static void drawSlot(GuiGraphics g, int x, int y, int border) {
		g.fill(x, y, x + 18, y + 18, 0xFF162033);
		g.renderOutline(x, y, 18, 18, border);
	}
}
