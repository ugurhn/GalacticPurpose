package com.galacticpurpose.client.screen;

import com.galacticpurpose.client.render.WormholeHologramRenderer;
import com.galacticpurpose.registry.ModBlocks;
import com.galacticpurpose.registry.ModItems;
import com.galacticpurpose.wormhole.ExoplanetRoute;
import com.galacticpurpose.wormhole.WormholeTier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * Interactive Visual Mission Codex & Engineering Tablet for Galactic Purpose:
 * 7 illustrated chapters with real 3D Minecraft item icons, live 3D Wormhole Hologram blueprint toggles,
 * universal RF/E + REI/JEI/EMI compatibility guide, and an interactive 3x3 visual crafting grid browser!
 */
public class MissionCodexScreen extends Screen {
	private static final int CHAPTER_COUNT = 7;

	private record VisualRecipe(String titleKey, Item result, int resultCount, Item[] grid9) {
	}

	private record HoverItem(int x, int y, ItemStack stack) {
	}

	private static List<VisualRecipe> recipes;

	private int selectedChapter = 0;
	private int selectedRecipe = 0;
	private int leftPos;
	private int topPos;
	private int panelW;
	private int panelH;

	private final List<HoverItem> hoverItems = new ArrayList<>();
	private Button holoPersonalBtn;
	private Button holoColossalBtn;
	private Button holoClearBtn;
	private Button prevRecipeBtn;
	private Button nextRecipeBtn;

	public MissionCodexScreen() {
		super(Component.translatable("codex.galacticpurpose.title"));
	}

	public static void open() {
		Minecraft.getInstance().setScreen(new MissionCodexScreen());
	}

	private static List<VisualRecipe> getRecipes() {
		if (recipes == null) {
			Item lead = ModItems.LEAD_INGOT;
			Item glass = Items.GLASS;
			Item lGlass = ModBlocks.LEADED_GLASS.asItem();
			Item concrete = ModBlocks.LEAD_CONCRETE.asItem();
			Item iron = Items.IRON_INGOT;
			Item copper = Items.COPPER_INGOT;
			Item copperBlk = Items.COPPER_BLOCK;
			Item red = Items.REDSTONE;
			Item redBlk = Items.REDSTONE_BLOCK;
			Item gold = Items.GOLD_INGOT;
			Item dia = Items.DIAMOND;
			Item blaze = Items.BLAZE_POWDER;
			Item ame = Items.AMETHYST_SHARD;
			Item a = Items.AIR;

			recipes = List.of(
				new VisualRecipe("block.galacticpurpose.solar_flux_generator", ModBlocks.SOLAR_FLUX_GENERATOR.asItem(), 1,
					new Item[]{glass, glass, glass, copper, red, copper, lead, iron, lead}),
				new VisualRecipe("item.galacticpurpose.lead_energy_cell", ModItems.LEAD_ENERGY_CELL, 1,
					new Item[]{a, copper, a, lead, red, lead, lead, red, lead}),
				new VisualRecipe("block.galacticpurpose.lead_concrete", concrete, 8,
					new Item[]{lead, Items.GRAVEL, lead, Items.GRAVEL, Items.SAND, Items.GRAVEL, lead, Items.GRAVEL, lead}),
				new VisualRecipe("block.galacticpurpose.thermal_heater", ModBlocks.THERMAL_HEATER.asItem(), 1,
					new Item[]{iron, copper, iron, blaze, Items.FURNACE, blaze, iron, red, iron}),
				new VisualRecipe("block.galacticpurpose.bio_distiller", ModBlocks.BIO_DISTILLER.asItem(), 1,
					new Item[]{lead, glass, lead, iron, Items.BREWING_STAND, iron, red, copper, red}),
				new VisualRecipe("block.galacticpurpose.deep_space_telescope", ModBlocks.DEEP_SPACE_TELESCOPE.asItem(), 1,
					new Item[]{a, lGlass, a, iron, Items.SPYGLASS, iron, lead, redBlk, lead}),
				new VisualRecipe("block.galacticpurpose.wormhole_core", ModBlocks.WORMHOLE_CORE.asItem(), 1,
					new Item[]{concrete, Items.ENDER_EYE, concrete, dia, ModItems.EXOTIC_BIO_FUEL, dia, concrete, redBlk, concrete}),
				new VisualRecipe("block.galacticpurpose.wormhole_stabilizer_pylon", ModBlocks.WORMHOLE_STABILIZER_PYLON.asItem(), 2,
					new Item[]{concrete, ame, concrete, ame, copperBlk, ame, concrete, redBlk, concrete}),
				new VisualRecipe("block.galacticpurpose.wormhole_injector", ModBlocks.WORMHOLE_INJECTOR.asItem(), 1,
					new Item[]{concrete, copperBlk, concrete, redBlk, dia, redBlk, concrete, copperBlk, concrete}),
				new VisualRecipe("item.galacticpurpose.thermal_chestplate", ModItems.THERMAL_CHESTPLATE, 1,
					new Item[]{gold, a, gold, lead, blaze, lead, lead, gold, lead}),
				new VisualRecipe("item.galacticpurpose.pressure_chestplate", ModItems.PRESSURE_CHESTPLATE, 1,
					new Item[]{copper, a, copper, lead, copper, lead, copper, lead, copper}),
				new VisualRecipe("item.galacticpurpose.quantum_core", ModItems.QUANTUM_CORE, 1,
					new Item[]{concrete, ModItems.QUANTUM_CRYSTAL, concrete,
						ModItems.FUSION_CORE, ModItems.BIO_STABILIZER, Items.ENDER_EYE,
						concrete, redBlk, concrete}),
				new VisualRecipe("block.galacticpurpose.ship_assembly_pad", ModBlocks.SHIP_ASSEMBLY_PAD.asItem(), 1,
					new Item[]{ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem(),
						copperBlk, redBlk, copperBlk,
						ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem()}),
				new VisualRecipe("item.galacticpurpose.starship_blueprint", ModItems.STARSHIP_BLUEPRINT, 1,
					new Item[]{ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.REINFORCED_VIEWPORT_GLASS.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem(),
						ModBlocks.STARSHIP_THRUSTER_BLOCK.asItem(), ModBlocks.STARSHIP_HULL_PLATING.asItem(), ModBlocks.STARSHIP_THRUSTER_BLOCK.asItem(),
						ModBlocks.STARSHIP_CARGO_LOCKER.asItem(), ModBlocks.STARSHIP_HELM.asItem(), ModBlocks.STARSHIP_CARGO_LOCKER.asItem()}),
				new VisualRecipe("item.galacticpurpose.starship_fuel_cell", ModItems.STARSHIP_FUEL_CELL, 2,
					new Item[]{a, copper, a, lead, ModItems.EXOTIC_BIO_FUEL, lead, a, copper, a})
			);
		}
		return recipes;
	}

	@Override
	protected void init() {
		this.panelW = Math.min(432, this.width - 10);
		this.panelH = Math.min(242, this.height - 10);
		this.leftPos = (this.width - this.panelW) / 2;
		this.topPos = (this.height - this.panelH) / 2;

		int navW = 94;
		int navX = this.leftPos + 6;
		for (int i = 0; i < CHAPTER_COUNT; i++) {
			final int chapterIdx = i;
			this.addRenderableWidget(Button.builder(
				Component.translatable("codex.galacticpurpose.tab." + i),
				btn -> selectChapter(chapterIdx)
			).bounds(navX, this.topPos + 24 + i * 26, navW, 20).build());
		}

		int cx = this.leftPos + 110;
		int cw = this.panelW - 118;
		int bottomY = this.topPos + this.panelH - 24;

		int btnW1 = Math.min(96, (cw - 8) / 3);
		int btnW2 = Math.min(112, (cw - 8) / 3 + 12);
		int btnW3 = Math.max(58, cw - btnW1 - btnW2 - 8);

		this.holoPersonalBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("codex.galacticpurpose.btn.holo_personal"),
			btn -> {
				WormholeHologramRenderer.projectInFrontOfPlayer(WormholeTier.PERSONAL);
				this.onClose();
			}
		).bounds(cx, bottomY, btnW1, 18).build());

		this.holoColossalBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("codex.galacticpurpose.btn.holo_colossal"),
			btn -> {
				WormholeHologramRenderer.projectInFrontOfPlayer(WormholeTier.COLOSSAL);
				this.onClose();
			}
		).bounds(cx + btnW1 + 4, bottomY, btnW2, 18).build());

		this.holoClearBtn = this.addRenderableWidget(Button.builder(
			Component.translatable("codex.galacticpurpose.btn.holo_clear"),
			btn -> {
				WormholeHologramRenderer.clear();
				this.onClose();
			}
		).bounds(cx + btnW1 + btnW2 + 8, bottomY, btnW3, 18).build());

		this.prevRecipeBtn = this.addRenderableWidget(Button.builder(
			Component.literal("<"),
			btn -> {
				List<VisualRecipe> list = getRecipes();
				this.selectedRecipe = (this.selectedRecipe - 1 + list.size()) % list.size();
			}
		).bounds(cx, bottomY, 28, 18).build());

		this.nextRecipeBtn = this.addRenderableWidget(Button.builder(
			Component.literal(">"),
			btn -> {
				List<VisualRecipe> list = getRecipes();
				this.selectedRecipe = (this.selectedRecipe + 1) % list.size();
			}
		).bounds(cx + cw - 28, bottomY, 28, 18).build());

		updateChapterButtons();
	}

	private void selectChapter(int chapter) {
		this.selectedChapter = chapter;
		updateChapterButtons();
	}

	private void updateChapterButtons() {
		boolean wormholeTab = this.selectedChapter == 4;
		boolean recipeTab = this.selectedChapter == 6;
		if (this.holoPersonalBtn != null) {
			this.holoPersonalBtn.visible = wormholeTab;
			this.holoColossalBtn.visible = wormholeTab;
			this.holoClearBtn.visible = wormholeTab;
		}
		if (this.prevRecipeBtn != null) {
			this.prevRecipeBtn.visible = recipeTab;
			this.nextRecipeBtn.visible = recipeTab;
		}
	}

	@Override
	public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		// In Minecraft 1.21.1, Screen.render() calls renderBackground() ONCE before widgets.
		// Drawing the Codex panel here (AFTER super.renderBackground blurs the world) keeps
		// the panel, text, icons, and buttons 100% razor-sharp even with Menu Blur enabled!
		super.renderBackground(g, mouseX, mouseY, partialTick);

		// Outer sci-fi cosmic observatory frame
		g.fill(this.leftPos, this.topPos, this.leftPos + this.panelW, this.topPos + this.panelH, 0xEE090E18);
		g.renderOutline(this.leftPos, this.topPos, this.panelW, this.panelH, 0xFF38BDF8);
		// Header bar & sidebar divider
		g.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + this.panelW - 1, this.topPos + 19, 0xFF101B2E);
		g.fill(this.leftPos + 103, this.topPos + 20, this.leftPos + 104, this.topPos + this.panelH - 2, 0xFF1E3A5F);

		// Active tab accent bar
		int activeY = this.topPos + 24 + this.selectedChapter * 26;
		g.fill(this.leftPos + 3, activeY, this.leftPos + 5, activeY + 20, 0xFFFACC15);

		g.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0xFFFACC15, true);
		g.drawString(this.font, Component.translatable("codex.galacticpurpose.subtitle"),
			this.leftPos + 110, this.topPos + 6, 0xFF94A3B8, false);
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
		this.hoverItems.clear();
		super.render(g, mouseX, mouseY, partialTick);

		int cx = this.leftPos + 110;
		int cy = this.topPos + 24;
		int cw = this.panelW - 118;

		switch (this.selectedChapter) {
			case 0 -> renderOverviewChapter(g, cx, cy, cw);
			case 1 -> renderEarthCollapseChapter(g, cx, cy, cw);
			case 2 -> renderMarsEnergyChapter(g, cx, cy, cw);
			case 3 -> renderRedGiantChapter(g, cx, cy, cw);
			case 4 -> renderWormholeChapter(g, cx, cy, cw);
			case 5 -> renderErebusExoplanetsChapter(g, cx, cy, cw);
			case 6 -> renderRecipesAndArmorChapter(g, cx, cy, cw);
		}

		// Render crisp hover tooltip for any visual item icon on the current page
		for (HoverItem hi : this.hoverItems) {
			if (mouseX >= hi.x && mouseX <= hi.x + 16 && mouseY >= hi.y && mouseY <= hi.y + 16) {
				g.renderTooltip(this.font, hi.stack, mouseX, mouseY);
				break;
			}
		}
	}

	private void renderOverviewChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch0.title", 0xFF38BDF8);
		int yy = drawWrappedCompact(g, x, y + 11, w, "codex.galacticpurpose.ch0.intro", 0xFFE2E8F0, 2, 9);

		Item[] stageIcons = {
			ModBlocks.SOLAR_TELEMETRY_MONITOR.asItem(),
			ModBlocks.THERMAL_HEATER.asItem(),
			ModItems.EXOTIC_BIO_FUEL,
			ModBlocks.WORMHOLE_CORE.asItem(),
			ModItems.QUANTUM_CORE
		};
		int[] stageColors = {0xFFEF4444, 0xFF22C55E, 0xFFF97316, 0xFFA855F7, 0xFF38BDF8};
		int rowY = yy + 4;
		for (int i = 0; i < 5; i++) {
			g.fill(x, rowY, x + w, rowY + 31, 0x88131E33);
			g.fill(x, rowY, x + 2, rowY + 31, stageColors[i]);
			drawIcon(g, x + 5, rowY + 7, new ItemStack(stageIcons[i]));
			drawClipped(g, x + 25, rowY + 2, w - 28, "codex.galacticpurpose.ch0.s" + (i + 1) + ".t", stageColors[i]);
			drawWrappedCompact(g, x + 25, rowY + 12, w - 28, "codex.galacticpurpose.ch0.s" + (i + 1) + ".d", 0xFFCBD5E1, 2, 9);
			rowY += 34;
		}
	}

	private void renderEarthCollapseChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch1.title", 0xFFEF4444);
		int yy = drawWrappedCompact(g, x, y + 11, w, "codex.galacticpurpose.ch1.body", 0xFFE2E8F0, 3, 9);

		String[] phases = {"p1", "p2", "p3", "p4"};
		int[] colors = {0xFF22C55E, 0xFFFACC15, 0xFFF97316, 0xFFEF4444};
		int py = yy + 5;
		for (int i = 0; i < 4; i++) {
			g.fill(x, py, x + w, py + 22, 0x88131E33);
			g.fill(x, py, x + 3, py + 22, colors[i]);
			drawWrappedCompact(g, x + 7, py + 2, w - 10, "codex.galacticpurpose.ch1." + phases[i], colors[i], 2, 9);
			py += 25;
		}

		// Visual key gear strip
		int gy = py + 6;
		drawClipped(g, x, gy, w, "codex.galacticpurpose.ch1.gear", 0xFFFACC15);
		drawIconRow(g, x, gy + 12,
			ModBlocks.LEAD_ORE.asItem(), ModItems.LEAD_INGOT, ModBlocks.LEAD_CONCRETE.asItem(),
			ModBlocks.SOLAR_FLUX_GENERATOR.asItem(), ModItems.LEAD_ENERGY_CELL, ModItems.THERMAL_CHESTPLATE);
	}

	private void renderMarsEnergyChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch2.title", 0xFF22C55E);
		int yy = drawWrappedCompact(g, x, y + 11, w, "codex.galacticpurpose.ch2.body", 0xFFE2E8F0, 2, 9);

		// 4 Terraforming Machines + Solar Flux Generator + Lead Energy Cell
		drawIconRow(g, x, yy + 4,
			ModBlocks.THERMAL_HEATER.asItem(), ModBlocks.PRESSURE_DRILL.asItem(),
			ModBlocks.FLORA_OXYGENATOR.asItem(), ModBlocks.ATMOSPHERE_CONDENSER.asItem(),
			ModBlocks.SOLAR_FLUX_GENERATOR.asItem(), ModItems.LEAD_ENERGY_CELL);

		int ty = yy + 27;
		g.fill(x, ty, x + w, ty + 46, 0x88131E33);
		g.renderOutline(x, ty, w, 46, 0xFF22C55E);
		drawClipped(g, x + 5, ty + 5, w - 10, "codex.galacticpurpose.ch2.ti1", 0xFF38BDF8);
		drawClipped(g, x + 5, ty + 18, w - 10, "codex.galacticpurpose.ch2.ti2", 0xFF4ADE80);
		drawClipped(g, x + 5, ty + 31, w - 10, "codex.galacticpurpose.ch2.ti3", 0xFFFACC15);

		int ey = ty + 52;
		g.fill(x, ey, x + w, ey + 66, 0x88131E33);
		g.renderOutline(x, ey, w, 66, 0xFF38BDF8);
		drawClipped(g, x + 5, ey + 4, w - 10, "codex.galacticpurpose.ch2.compat_title", 0xFF38BDF8);
		drawWrappedCompact(g, x + 5, ey + 15, w - 10, "codex.galacticpurpose.ch2.compat_body", 0xFFCBD5E1, 5, 9);
	}

	private void renderRedGiantChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch3.title", 0xFFF97316);
		int yy = drawWrappedCompact(g, x, y + 11, w, "codex.galacticpurpose.ch3.body", 0xFFE2E8F0, 2, 9);

		// Visual Fuel Chain: Bio-Bloom -> 2x Bio-Essence -> Bio-Distiller -> Exotic Bio-Fuel
		int fy = yy + 6;
		g.fill(x, fy, x + w, fy + 36, 0x88131E33);
		drawClipped(g, x + 5, fy + 3, w - 10, "codex.galacticpurpose.ch3.chain", 0xFFFACC15);
		drawIcon(g, x + 6, fy + 15, new ItemStack(ModBlocks.MARS_BIO_BLOOM));
		g.drawString(this.font, "->", x + 26, fy + 19, 0xFF94A3B8, false);
		drawIcon(g, x + 40, fy + 15, new ItemStack(ModItems.MARS_BIO_ESSENCE, 2));
		g.drawString(this.font, "->", x + 60, fy + 19, 0xFF94A3B8, false);
		drawIcon(g, x + 74, fy + 15, new ItemStack(ModBlocks.BIO_DISTILLER));
		g.drawString(this.font, "->", x + 94, fy + 19, 0xFF94A3B8, false);
		drawIcon(g, x + 108, fy + 15, new ItemStack(ModItems.EXOTIC_BIO_FUEL));
		g.drawString(this.font, "+", x + 130, fy + 19, 0xFF94A3B8, false);
		drawIcon(g, x + 142, fy + 15, new ItemStack(ModBlocks.DEEP_SPACE_TELESCOPE));
		g.drawString(this.font, "->", x + 162, fy + 19, 0xFF94A3B8, false);
		drawIcon(g, x + 176, fy + 15, new ItemStack(ModItems.forCoordinateCard(ExoplanetRoute.HEPHAESTUS_IV)));

		// Radiation Bunker + Lead Suit box
		int by = fy + 42;
		g.fill(x, by, x + w, by + 84, 0x88131E33);
		g.renderOutline(x, by, w, 84, 0xFFF97316);
		drawClipped(g, x + 5, by + 4, w - 10, "codex.galacticpurpose.ch3.storm_title", 0xFFF97316);
		drawWrappedCompact(g, x + 5, by + 15, w - 10, "codex.galacticpurpose.ch3.storm_body", 0xFFCBD5E1, 4, 9);
		drawIconRow(g, x + 5, by + 60,
			ModBlocks.SOLAR_TELEMETRY_MONITOR.asItem(), ModBlocks.LEAD_CONCRETE.asItem(),
			ModBlocks.LEADED_GLASS.asItem(), ModItems.LEAD_HELMET, ModItems.LEAD_CHESTPLATE);
	}

	private void renderWormholeChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch4.title", 0xFFA855F7);

		// Two-tier comparison cards
		int cardW = (w - 6) / 2;
		int cy = y + 12;
		g.fill(x, cy, x + cardW, cy + 78, 0x88131E33);
		g.renderOutline(x, cy, cardW, 78, 0xFF38BDF8);
		drawClipped(g, x + 4, cy + 4, cardW - 8, "codex.galacticpurpose.ch4.t1_title", 0xFF38BDF8);
		drawWrappedCompact(g, x + 4, cy + 15, cardW - 8, "codex.galacticpurpose.ch4.t1_body", 0xFFCBD5E1, 6, 9);

		int rx = x + cardW + 6;
		g.fill(rx, cy, rx + cardW, cy + 78, 0x88131E33);
		g.renderOutline(rx, cy, cardW, 78, 0xFFFACC15);
		drawClipped(g, rx + 4, cy + 4, cardW - 8, "codex.galacticpurpose.ch4.t2_title", 0xFFFACC15);
		drawWrappedCompact(g, rx + 4, cy + 15, cardW - 8, "codex.galacticpurpose.ch4.t2_body", 0xFFCBD5E1, 6, 9);

		// Required blocks strip + Hologram color legend
		int ly = cy + 83;
		drawIconRow(g, x, ly,
			ModBlocks.WORMHOLE_CORE.asItem(), ModBlocks.WORMHOLE_STABILIZER_PYLON.asItem(),
			ModBlocks.WORMHOLE_INJECTOR.asItem(), ModBlocks.LEAD_CONCRETE.asItem(),
			Items.BLUE_ICE, ModItems.EXOTIC_BIO_FUEL);

		int wy = ly + 22;
		g.fill(x, wy, x + w, wy + 56, 0x88240C10);
		g.renderOutline(x, wy, w, 56, 0xFFEF4444);
		drawClipped(g, x + 5, wy + 4, w - 10, "codex.galacticpurpose.ch4.warn_title", 0xFFEF4444);
		drawWrappedCompact(g, x + 5, wy + 15, w - 10, "codex.galacticpurpose.ch4.warn_body", 0xFFFECACA, 4, 9);
	}

	private void renderErebusExoplanetsChapter(GuiGraphics g, int x, int y, int w) {
		drawHeader(g, x, y, "codex.galacticpurpose.ch5.title", 0xFFFACC15);
		int yy = drawWrappedCompact(g, x, y + 11, w, "codex.galacticpurpose.ch5.intro", 0xFFCBD5E1, 2, 9);

		Item[] planetLoot = {ModItems.FUSION_CORE, ModItems.BIO_STABILIZER, ModItems.QUANTUM_CRYSTAL, ModItems.QUANTUM_CORE};
		Item[] planetSuit = {ModItems.THERMAL_CHESTPLATE, ModItems.PRESSURE_CHESTPLATE, ModItems.THERMAL_CHESTPLATE, Items.GRASS_BLOCK};
		int[] colors = {0xFFF97316, 0xFF0EA5E9, 0xFFA855F7, 0xFF22C55E};

		int ry = yy + 4;
		for (int i = 0; i < 4; i++) {
			g.fill(x, ry, x + w, ry + 36, 0x88131E33);
			g.fill(x, ry, x + 3, ry + 36, colors[i]);
			drawIcon(g, x + 6, ry + 9, new ItemStack(planetSuit[i]));
			drawIcon(g, x + w - 22, ry + 9, new ItemStack(planetLoot[i]));
			drawClipped(g, x + 26, ry + 3, w - 52, "codex.galacticpurpose.ch5.p" + (i + 1) + ".t", colors[i]);
			drawWrappedCompact(g, x + 26, ry + 13, w - 52, "codex.galacticpurpose.ch5.p" + (i + 1) + ".d", 0xFFE2E8F0, 2, 9);
			ry += 39;
		}
	}

	private void renderRecipesAndArmorChapter(GuiGraphics g, int x, int y, int w) {
		List<VisualRecipe> list = getRecipes();
		VisualRecipe rec = list.get(this.selectedRecipe % list.size());

		drawHeader(g, x, y, "codex.galacticpurpose.ch6.title", 0xFF38BDF8);
		g.drawString(this.font,
			Component.literal((this.selectedRecipe + 1) + "/" + list.size() + " · ")
				.append(Component.translatable(rec.titleKey())),
			x, y + 12, 0xFFFACC15, false);

		// 3x3 Visual Crafting Grid!
		int gx = x + 8;
		int gy = y + 25;
		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 3; col++) {
				int sx = gx + col * 20;
				int sy = gy + row * 20;
				g.fill(sx, sy, sx + 18, sy + 18, 0xFF1A263C);
				g.renderOutline(sx, sy, 18, 18, 0xFF3B527A);
				Item it = rec.grid9()[row * 3 + col];
				if (it != Items.AIR) {
					drawIcon(g, sx + 1, sy + 1, new ItemStack(it));
				}
			}
		}

		// Arrow + Output slot
		g.drawString(this.font, "===>", gx + 70, gy + 25, 0xFFFACC15, true);
		int outX = gx + 104;
		int outY = gy + 18;
		g.fill(outX, outY, outX + 22, outY + 22, 0xFF1E293B);
		g.renderOutline(outX, outY, 22, 22, 0xFFFACC15);
		ItemStack resultStack = new ItemStack(rec.result(), rec.resultCount());
		drawIcon(g, outX + 3, outY + 3, resultStack);
		if (rec.resultCount() > 1) {
			g.drawString(this.font, "x" + rec.resultCount(), outX + 26, outY + 7, 0xFFFFFFFF, true);
		}

		// Anti-God-Mode Armor Matrix below the recipe grid
		int ay = gy + 66;
		g.fill(x, ay, x + w, ay + 84, 0x88131E33);
		g.renderOutline(x, ay, w, 84, 0xFF38BDF8);
		drawClipped(g, x + 5, ay + 4, w - 10, "codex.galacticpurpose.ch6.armor_title", 0xFF38BDF8);
		drawIcon(g, x + 5, ay + 18, new ItemStack(ModItems.LEAD_CHESTPLATE));
		drawWrappedCompact(g, x + 25, ay + 17, w - 30, "codex.galacticpurpose.ch6.a1", 0xFFE2E8F0, 2, 9);
		drawIcon(g, x + 5, ay + 40, new ItemStack(ModItems.THERMAL_CHESTPLATE));
		drawWrappedCompact(g, x + 25, ay + 39, w - 30, "codex.galacticpurpose.ch6.a2", 0xFFFDBA74, 2, 9);
		drawIcon(g, x + 5, ay + 62, new ItemStack(ModItems.PRESSURE_CHESTPLATE));
		drawWrappedCompact(g, x + 25, ay + 61, w - 30, "codex.galacticpurpose.ch6.a3", 0xFF7DD3FC, 2, 9);
	}

	private void drawHeader(GuiGraphics g, int x, int y, String key, int color) {
		g.drawString(this.font, Component.translatable(key), x, y, color, true);
	}

	private void drawClipped(GuiGraphics g, int x, int y, int maxWidth, String key, int color) {
		String raw = Component.translatable(key).getString();
		String clipped = this.font.plainSubstrByWidth(raw, maxWidth);
		g.drawString(this.font, clipped, x, y, color, false);
	}

	private int drawWrappedCompact(GuiGraphics g, int x, int y, int width, String key, int color, int maxLines, int lineHeight) {
		List<FormattedCharSequence> lines = this.font.split(Component.translatable(key), width);
		int curY = y;
		int count = Math.min(lines.size(), maxLines);
		for (int i = 0; i < count; i++) {
			g.drawString(this.font, lines.get(i), x, curY, color, false);
			curY += lineHeight;
		}
		return curY;
	}

	private void drawIconRow(GuiGraphics g, int x, int y, Item... items) {
		for (int i = 0; i < items.length; i++) {
			int ix = x + i * 22;
			g.fill(ix - 1, y - 1, ix + 17, y + 17, 0xAA162238);
			g.renderOutline(ix - 1, y - 1, 18, 18, 0xFF334155);
			drawIcon(g, ix, y, new ItemStack(items[i]));
		}
	}

	private void drawIcon(GuiGraphics g, int x, int y, ItemStack stack) {
		g.renderItem(stack, x, y);
		g.renderItemDecorations(this.font, stack, x, y);
		this.hoverItems.add(new HoverItem(x, y, stack));
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
