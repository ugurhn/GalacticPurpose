package com.galacticpurpose.client.screen;

import com.galacticpurpose.menu.WormholeCoreMenu;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeStabilityCalculator;
import com.galacticpurpose.wormhole.WormholeState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Containment control UI for the Wormhole Stabilizer Core multiblock.
 */
public class WormholeCoreScreen extends AbstractContainerScreen<WormholeCoreMenu> {
	private Button igniteButton;
	private Button quenchButton;
	private Button enterButton;

	public WormholeCoreScreen(WormholeCoreMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
		this.imageWidth = 176;
		this.imageHeight = 202;
		this.titleLabelX = 8;
		this.titleLabelY = 5;
		this.inventoryLabelX = 8;
		this.inventoryLabelY = 109;
	}

	@Override
	protected void init() {
		super.init();
		int y = this.topPos + 87;
		this.igniteButton = this.addRenderableWidget(
			Button.builder(Component.translatable("gui.galacticpurpose.wormhole_btn_ignite"), b -> pressButton(0))
				.bounds(this.leftPos + 8, y, 50, 16)
				.build()
		);
		this.quenchButton = this.addRenderableWidget(
			Button.builder(Component.translatable("gui.galacticpurpose.wormhole_btn_quench"), b -> pressButton(1))
				.bounds(this.leftPos + 63, y, 50, 16)
				.build()
		);
		this.enterButton = this.addRenderableWidget(
			Button.builder(Component.translatable("gui.galacticpurpose.wormhole_btn_enter"), b -> pressButton(2))
				.bounds(this.leftPos + 118, y, 50, 16)
				.build()
		);
		updateButtonStates();
	}

	@Override
	protected void containerTick() {
		super.containerTick();
		updateButtonStates();
	}

	private void pressButton(int buttonId) {
		if (this.minecraft != null && this.minecraft.gameMode != null) {
			this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
		}
	}

	private void updateButtonStates() {
		WormholeState state = this.menu.wormholeState();
		if (this.igniteButton != null) {
			this.igniteButton.active = state == WormholeState.IDLE || state == WormholeState.SHUTTING_DOWN;
		}
		if (this.quenchButton != null) {
			this.quenchButton.active = state != WormholeState.IDLE && state != WormholeState.SHUTTING_DOWN;
		}
		if (this.enterButton != null) {
			this.enterButton.active = state == WormholeState.OPEN && this.menu.targetRoute() != null;
		}
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
		int x = this.leftPos;
		int y = this.topPos;

		graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF1A202C);
		graphics.renderOutline(x, y, this.imageWidth, this.imageHeight, 0xFF4A5568);
		graphics.fill(x + 4, y + 16, x + this.imageWidth - 4, y + 106, 0xFF11161F);

		// Slots: 0=Card (10,22), 1=Fuel (10,44), 2=Coolant (10,66), 3=Battery (30,66)
		drawSlotBox(graphics, x + 9, y + 21, 0xFF8B5CF6);
		drawSlotBox(graphics, x + 9, y + 43, 0xFFC026D3);
		drawSlotBox(graphics, x + 9, y + 65, 0xFF38BDF8);
		drawSlotBox(graphics, x + 29, y + 65, 0xFF00D4FF);

		// Energy bar (x + 51, y + 21, width 8, height 62)
		int barX = x + 51;
		int barY = y + 21;
		int barW = 8;
		int barH = 62;
		graphics.fill(barX, barY, barX + barW, barY + barH, 0xFF0A0D12);
		graphics.renderOutline(barX, barY, barW, barH, 0xFF334155);
		float energyRatio = clamp01(this.menu.energyStored() / (float) this.menu.energyCapacity());
		int filledH = Math.round((barH - 2) * energyRatio);
		if (filledH > 0) {
			graphics.fill(barX + 1, barY + barH - 1 - filledH, barX + barW - 1, barY + barH - 1, 0xFF00D4FF);
		}

		// Telemetry bars (x + 63, w = 105)
		ExoplanetRoute route = this.menu.targetRoute();
		int chargeColor = route != null ? (0xFF000000 | route.accentRgb()) : 0xFFA855F7;
		drawMetricBar(graphics, x + 63, y + 40, 105, 10, this.menu.chargePercent() / 100.0f, chargeColor);

		float stability = this.menu.stabilityPercent();
		int stabColor = stability >= 70.0f ? 0xFF22C55E : (stability >= 35.0f ? 0xFFF59E0B : 0xFFEF4444);
		drawMetricBar(graphics, x + 63, y + 51, 105, 10, stability / 100.0f, stabColor);

		int temp = this.menu.tempCelsius();
		int tempColor = temp >= WormholeStabilityCalculator.DANGER_TEMP ? 0xFFEF4444
			: (temp >= WormholeStabilityCalculator.COOLANT_TRIGGER_TEMP ? 0xFFF59E0B : 0xFFFF6B35);
		drawMetricBar(
			graphics,
			x + 63,
			y + 62,
			105,
			10,
			temp / WormholeStabilityCalculator.MELTDOWN_TEMP,
			tempColor
		);

		drawMetricBar(graphics, x + 63, y + 73, 51, 10, this.menu.fuelUnits() / 1000.0f, 0xFFD946EF);
		drawMetricBar(graphics, x + 117, y + 73, 51, 10, this.menu.coolantUnits() / 1000.0f, 0xFF38BDF8);

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				drawSlotBox(graphics, x + 7 + col * 18, y + 119 + row * 18, 0xFF2D3748);
			}
		}
		for (int col = 0; col < 9; col++) {
			drawSlotBox(graphics, x + 7 + col * 18, y + 177, 0xFF2D3748);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xF1F5F9, false);
		graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xCBD5E1, false);

		WormholeState state = this.menu.wormholeState();
		graphics.drawString(
			this.font,
			Component.translatable(state.translationKey()),
			63,
			19,
			state.accentRgb(),
			false
		);

		Component badge = Component.translatable(
			this.menu.isStructureValid()
				? "gui.galacticpurpose.wormhole_struct_ok"
				: "gui.galacticpurpose.wormhole_struct_bad"
		);
		int badgeColor = this.menu.isStructureValid() ? 0x22C55E : 0xEF4444;
		int badgeX = 168 - this.font.width(badge);
		graphics.drawString(this.font, badge, badgeX, 19, badgeColor, false);
		if (this.menu.isQuantumUpgraded()) {
			Component qBadge = Component.literal("[Q]");
			graphics.drawString(this.font, qBadge, badgeX - this.font.width(qBadge) - 3, 19, 0xD946EF, false);
		}

		ExoplanetRoute route = this.menu.targetRoute();
		Component routeLabel = route != null
			? Component.translatable(route.translationKey())
			: Component.translatable("gui.galacticpurpose.wormhole_no_card");
		int routeColor = route != null ? route.accentRgb() : 0x94A3B8;
		graphics.drawString(this.font, routeLabel, 63, 29, routeColor, false);

		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.wormhole_charge", String.format("%.0f", this.menu.chargePercent())),
			66,
			41,
			0xFFFFFF,
			true
		);
		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.wormhole_stability", String.format("%.0f", this.menu.stabilityPercent())),
			66,
			52,
			0xFFFFFF,
			true
		);
		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.wormhole_temp", this.menu.tempCelsius()),
			66,
			63,
			0xFFFFFF,
			true
		);
		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.wormhole_fuel_short", this.menu.fuelUnits()),
			65,
			74,
			0xFFFFFF,
			true
		);
		graphics.drawString(
			this.font,
			Component.translatable("gui.galacticpurpose.wormhole_cool_short", this.menu.coolantUnits()),
			119,
			74,
			0xFFFFFF,
			true
		);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);

		int barX = this.leftPos + 51;
		int barY = this.topPos + 21;
		if (mouseX >= barX && mouseX <= barX + 8 && mouseY >= barY && mouseY <= barY + 62) {
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
			return;
		}

		if (this.hoveredSlot != null && !this.hoveredSlot.hasItem() && this.hoveredSlot.index < 4) {
			String key = switch (this.hoveredSlot.index) {
				case 0 -> "gui.galacticpurpose.slot_card";
				case 1 -> "gui.galacticpurpose.slot_fuel";
				case 2 -> "gui.galacticpurpose.slot_coolant";
				case 3 -> "gui.galacticpurpose.slot_battery";
				default -> null;
			};
			if (key != null) {
				graphics.renderTooltip(this.font, Component.translatable(key), mouseX, mouseY);
				return;
			}
		}

		this.renderTooltip(graphics, mouseX, mouseY);
	}

	private static float clamp01(float value) {
		return Math.min(1.0f, Math.max(0.0f, value));
	}

	private static void drawSlotBox(GuiGraphics graphics, int x, int y, int borderColor) {
		graphics.fill(x, y, x + 18, y + 18, borderColor);
		graphics.fill(x + 1, y + 1, x + 17, y + 17, 0xFF0F172A);
	}

	private static void drawMetricBar(
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
		int filled = Math.round((width - 2) * clamp01(ratio));
		if (filled > 0) {
			graphics.fill(x + 1, y + 1, x + 1 + filled, y + height - 1, fillColor);
		}
	}
}
