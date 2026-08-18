package strhercules.chickens.registry;

import strhercules.chickens.ChickensMod;
import strhercules.chickens.menu.AvianChemicalConverterMenu;
import strhercules.chickens.menu.AvianDousingMachineMenu;
import strhercules.chickens.menu.AvianFluxConverterMenu;
import strhercules.chickens.menu.AvianFluidConverterMenu;
import strhercules.chickens.menu.BreederMenu;
import strhercules.chickens.menu.CollectorMenu;
import strhercules.chickens.menu.IncubatorMenu;
import strhercules.chickens.menu.HenhouseMenu;
import strhercules.chickens.menu.MegaChickenMenu;
import strhercules.chickens.menu.RoostMenu;
import strhercules.chickens.menu.MechanicalRoostMenu;
import strhercules.chickens.menu.MechanicalNestMenu;
import strhercules.chickens.menu.NestMenu;
import strhercules.chickens.menu.RoosterMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/**
 * Central point for container menu registrations. Each entry pairs with a
 * client-side screen registered in {@link strhercules.chickens.ChickensClient}.
 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, ChickensMod.MOD_ID);

    public static final RegistryObject<MenuType<HenhouseMenu>> HENHOUSE = MENU_TYPES.register("henhouse",
            () -> IForgeMenuType.create(HenhouseMenu::new));
    public static final RegistryObject<MenuType<RoostMenu>> ROOST = MENU_TYPES.register("roost",
            () -> IForgeMenuType.create(RoostMenu::new));
    public static final RegistryObject<MenuType<MechanicalRoostMenu>> MECHANICAL_ROOST = MENU_TYPES.register(
            "mechanical_roost", () -> IForgeMenuType.create(MechanicalRoostMenu::new));
    public static final RegistryObject<MenuType<MechanicalNestMenu>> MECHANICAL_NEST = MENU_TYPES.register(
            "mechanical_nest", () -> IForgeMenuType.create(MechanicalNestMenu::new));
    public static final RegistryObject<MenuType<NestMenu>> NEST = MENU_TYPES.register("nest",
            () -> IForgeMenuType.create(NestMenu::new));
    public static final RegistryObject<MenuType<RoosterMenu>> ROOSTER = MENU_TYPES.register("rooster",
            () -> IForgeMenuType.create(RoosterMenu::new));
    public static final RegistryObject<MenuType<BreederMenu>> BREEDER = MENU_TYPES.register("breeder",
            () -> IForgeMenuType.create(BreederMenu::new));
    public static final RegistryObject<MenuType<CollectorMenu>> COLLECTOR = MENU_TYPES.register("collector",
            () -> IForgeMenuType.create(CollectorMenu::new));
    public static final RegistryObject<MenuType<AvianFluxConverterMenu>> AVIAN_FLUX_CONVERTER = MENU_TYPES.register("avian_flux_converter",
            () -> IForgeMenuType.create(AvianFluxConverterMenu::new));
    public static final RegistryObject<MenuType<AvianFluidConverterMenu>> AVIAN_FLUID_CONVERTER = MENU_TYPES.register("avian_fluid_converter",
            () -> IForgeMenuType.create(AvianFluidConverterMenu::new));
    public static final RegistryObject<MenuType<AvianChemicalConverterMenu>> AVIAN_CHEMICAL_CONVERTER = MENU_TYPES.register("avian_chemical_converter",
            () -> IForgeMenuType.create(AvianChemicalConverterMenu::new));
    public static final RegistryObject<MenuType<AvianDousingMachineMenu>> AVIAN_DOUSING_MACHINE = MENU_TYPES.register("avian_dousing_machine",
            () -> IForgeMenuType.create(AvianDousingMachineMenu::new));
    public static final RegistryObject<MenuType<IncubatorMenu>> INCUBATOR = MENU_TYPES.register("incubator",
            () -> IForgeMenuType.create(IncubatorMenu::new));
    public static final RegistryObject<MenuType<MegaChickenMenu>> MEGA_CHICKEN = MENU_TYPES.register("mega_chicken",
            () -> IForgeMenuType.create(MegaChickenMenu::new));

    private ModMenuTypes() {
    }

    public static void init(IEventBus modBus) {
        MENU_TYPES.register(modBus);
    }
}
