package com.galacticpurpose.registry;

import com.galacticpurpose.GalacticPurpose;
import com.galacticpurpose.menu.BioDistillerMenu;
import com.galacticpurpose.menu.StarshipMenu;
import com.galacticpurpose.menu.TerraformingMachineMenu;
import com.galacticpurpose.menu.WormholeCoreMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.inventory.MenuType;

/**
 * Registers server/client screen handler menu types.
 */
public final class ModMenus {
	public static final MenuType<TerraformingMachineMenu> TERRAFORMING_MACHINE = Registry.register(
		BuiltInRegistries.MENU,
		GalacticPurpose.id("terraforming_machine"),
		new ExtendedScreenHandlerType<>(TerraformingMachineMenu::new, BlockPos.STREAM_CODEC)
	);
	public static final MenuType<BioDistillerMenu> BIO_DISTILLER = Registry.register(
		BuiltInRegistries.MENU,
		GalacticPurpose.id("bio_distiller"),
		new ExtendedScreenHandlerType<>(BioDistillerMenu::new, BlockPos.STREAM_CODEC)
	);
	public static final MenuType<WormholeCoreMenu> WORMHOLE_CORE = Registry.register(
		BuiltInRegistries.MENU,
		GalacticPurpose.id("wormhole_core"),
		new ExtendedScreenHandlerType<>(WormholeCoreMenu::new, BlockPos.STREAM_CODEC)
	);
	public static final MenuType<StarshipMenu> STARSHIP = Registry.register(
		BuiltInRegistries.MENU,
		GalacticPurpose.id("starship"),
		new ExtendedScreenHandlerType<>(StarshipMenu::new, ByteBufCodecs.VAR_INT)
	);

	private ModMenus() {
	}

	public static void register() {
		// Class initialization performs static registry bindings.
	}
}
