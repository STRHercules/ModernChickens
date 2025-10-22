package com.setycz.chickens.registry;

import com.setycz.chickens.ChickensMod;
import com.setycz.chickens.menu.AvianFluxConverterMenu;
import com.setycz.chickens.menu.BreederMenu;
import com.setycz.chickens.menu.CollectorMenu;
import com.setycz.chickens.menu.HenhouseMenu;
import com.setycz.chickens.menu.RoostMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Central point for container menu registrations. Each entry pairs with a
 * client-side screen registered in {@link com.setycz.chickens.ChickensClient}.
 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ChickensMod.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<HenhouseMenu>> HENHOUSE = MENU_TYPES.register("henhouse",
            () -> IMenuTypeExtension.create(HenhouseMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RoostMenu>> ROOST = MENU_TYPES.register("roost",
            () -> IMenuTypeExtension.create(RoostMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BreederMenu>> BREEDER = MENU_TYPES.register("breeder",
            () -> IMenuTypeExtension.create(BreederMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CollectorMenu>> COLLECTOR = MENU_TYPES.register("collector",
            () -> IMenuTypeExtension.create(CollectorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<AvianFluxConverterMenu>> AVIAN_FLUX_CONVERTER = MENU_TYPES.register("avian_flux_converter",
            () -> IMenuTypeExtension.create(AvianFluxConverterMenu::new));

    private ModMenuTypes() {
    }

    public static void init(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
