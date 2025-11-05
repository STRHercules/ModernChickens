package com.modernfluidcows.config;

import com.modernfluidcows.ModernFluidCows;
import com.modernfluidcows.util.FCUtils;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.Fluid;

/**
 * Port of the legacy FluidCows JSON configuration loader.
 *
 * <p>This class mirrors the original field layout so gameplay code can be migrated gradually. It
 * translates the 1.12 string-based fluid identifiers to modern {@link ResourceLocation} keys while
 * preserving the default values.</p>
 */
public final class FCConfig {
    public static final String COMMENT = "_Comment";
    public static final String RATE = "SpawnRate";
    public static final String ENABLE = "IsEnabled";
    public static final String WORLD = "WorldCooldown";
    public static final String STALL = "StallCooldown";
    public static final String BREEDING_CHANCE = "BreedingChance";
    public static final String BREEDING_COOLDOWN = "BreedingCooldown";
    public static final String GROWING_BABY = "GrowingAge";
    public static final String PARENT_1 = "Parent First";
    public static final String PARENT_2 = "Parent Second";

    public static final String COMMENT_CLIENT = "_Comment Client";
    public static final String CLIENT = "Client";
    public static final String HIDEFLUIDCOW = "HideFluidLayerCow";

    public static final String COMMENT_GENERAL = "_Comment General";
    public static final String GENERAL = "General";
    public static final String BREEDING = "BreedingItemWork";
    public static final String PROJECTETICK = "ProjectETickRemove";
    public static final String NOTENOWANDSTICK = "NotEnoughtWandsTickRemove";
    public static final String TORCHERINOTICK = "TorcherinoTickRemove";
    public static final String RANDOMTHINGSTICK = "randomthingsTickRemove";
    public static final String BREEDINGITEMMACHINES = "DisableBreedingItemForMachines";
    public static final String SPAWNWEIGHT = "FluidCowsSpawnWeight";
    public static final String SPAWNMIN = "FluidCowsSpawnMin";
    public static final String SPAWNMAX = "FluidCowsSpawnMax";
    public static final String SPAWNBLACKLIST = "FluidCowsSpawnBlackListBiomes";
    public static final String ACCELERATORMAX = "AcceleratorMaxSubstance";
    public static final String ACCELERATORRADIUS = "AcceleratorRadius";
    public static final String ACCELERATORPERCOW = "AcceleratorSubstancePerCow";
    public static final String ACCELERATORMULTIPLIER = "AcceleratorMultiplier";
    public static final String ACCELERATORWATER = "AcceleratorWaterPerConvert";
    public static final String BLACKLISTDIMIDS = "BlackListDimIds";
    public static final String ENABLECONVERTCOWTODISPLAYER = "EnableConvertCowToDisplayer";
    public static final String BLACKLISTCOWTODISPLAYER = "BlackListCowToDisplayer";
    public static final String FEEDERBLACKLIST = "FeederBlackList";
    public static final String EIOBLACKLISTSPAWNING = "EIOBlackListSpawning";
    public static final String EIOBLACKLISTSOULVIAL = "EIOBlackListSoulVial";
    public static final String EIONEEDSCLONING = "EIONeedsCloning";
    public static final String EIOENTITYCOSTMULTIPLIER = "EIOEntityCostMultiplier";

    private static final FluidInfo DEFAULT_FLUID =
            new FluidInfo(0, false, Integer.MAX_VALUE, Integer.MAX_VALUE, 0, Integer.MAX_VALUE, Integer.MIN_VALUE);

    private static JsonConfig parser;

    public static final List<Fluid> FLUIDS = new ArrayList<>();
    public static int sumWeight;

    public static boolean hideFluidCow;

    public static final Map<CustomPair<ResourceLocation, ResourceLocation>, List<ResourceLocation>> breed =
            new LinkedHashMap<>();
    public static final Set<ResourceLocation> canBreed = new HashSet<>();
    public static boolean breedingItemWork;
    public static boolean projecteTickRemove;
    public static boolean notenoughwandsTickRemove;
    public static boolean torcherinoTickRemove;
    public static boolean randomthingsTickRemove;
    public static boolean disableBreedingItemForMachines;
    public static int spawnWeight;
    public static int spawnMin;
    public static int spawnMax;
    public static String[] spawnBlackListBiomes;
    public static Set<ResourceLocation> spawnBlackListBiomeKeys = Set.of();
    public static int acceleratorMax;
    public static int acceleratorRadius;
    public static int acceleratorPerCow;
    public static int acceleratorMultiplier;
    public static int acceleratorWater;
    public static Set<Integer> blackListDimIds = Set.of();
    public static boolean enableConvertCowToDisplayer;
    public static Set<String> blackListCowToDisplayer = Set.of();
    public static Set<String> feederBlackList = Set.of();

    public static boolean EIOBlackListSpawning;
    public static boolean EIOBlackListSoulVial;
    public static boolean EIONeedsCloning;
    public static int EIOEntityCostMultiplier;

    private static final Map<ResourceLocation, FluidInfo> REGISTRY = new HashMap<>();

    public static boolean loaded;

    private FCConfig() {}

    public static void setFile(final Path file) {
        parser = new JsonConfig(file);
    }

    public static void load() {
        if (parser == null) {
            throw new IllegalStateException("Config file not initialised; call setFile before load.");
        }

        parser.load();

        parser.getOrDefault(COMMENT, RATE, "Spawn rate");
        parser.getOrDefault(COMMENT, ENABLE, "False = Disabled, true = Enabled");
        parser.getOrDefault(COMMENT, WORLD, "Cooldown if cow milked in world (not in stall, like mechanisms)");
        parser.getOrDefault(COMMENT, STALL, "Cooldown if cow milked from Stall");
        parser.getOrDefault(COMMENT, BREEDING_CHANCE, "Chance of breeding to succeed");
        parser.getOrDefault(COMMENT, BREEDING_COOLDOWN, "How many ticks it takes before the cow can breed again");
        parser.getOrDefault(COMMENT, GROWING_BABY, "How many ticks it takes for the baby cow to grow up");
        parser.getOrDefault(COMMENT, PARENT_1, "First parent to fluid (empty is disable) example usage: \"lava\" \"water\"");
        parser.getOrDefault(COMMENT, PARENT_2, "Second parent to fluid (empty is disable) example usage: \"lava\" \"water\"");
        parser.getOrDefault(COMMENT, "Tip#1", "Cow rewards? Yes! Set enable to true, rate to zero, remove parents and make recipe with CraftTweaker!");
        parser.getOrDefault(COMMENT, "Tip#2", "Only breeding cow? Yes! Set enable to true, rate to zero and add parents!");

        parser.getOrDefault(COMMENT_CLIENT, HIDEFLUIDCOW, "Disable fluid render layer cow in stall");

        hideFluidCow = parser.getOrDefault(CLIENT, HIDEFLUIDCOW, false);

        parser.getOrDefault(COMMENT_GENERAL, BREEDING, "If true u can use the breeding item to get lower baby growing age");
        parser.getOrDefault(COMMENT_GENERAL, PROJECTETICK, "If true - \"Watch of Flowing Time\" not work on Cow Stall. From mod \"ProjectE\"");
        parser.getOrDefault(COMMENT_GENERAL, NOTENOWANDSTICK, "If true - \"Acceleration Wand\" not work on Cow Stall. From mod \"Not Enough Wand\"");
        parser.getOrDefault(COMMENT_GENERAL, TORCHERINOTICK, "If true - all types \"Torcherino\" not work on Cow Stall. From mod \"Torcherino\"");
        parser.getOrDefault(COMMENT_GENERAL, RANDOMTHINGSTICK, "If true - \"Time in a bottle\" not work on Cow Stall. From mod \"Random Things\"");
        parser.getOrDefault(COMMENT_GENERAL, BREEDINGITEMMACHINES, "Disables get breeding item via machines");

        parser.getOrDefault(COMMENT_GENERAL, SPAWNWEIGHT, "Fluid cows spawn weight");
        parser.getOrDefault(COMMENT_GENERAL, SPAWNMIN, "Fluid cows spawn min");
        parser.getOrDefault(COMMENT_GENERAL, SPAWNMAX, "Fluid cows spawn max");
        parser.getOrDefault(COMMENT_GENERAL, SPAWNBLACKLIST, "Fluid cows spawn black list biomes (modid:name)");

        parser.getOrDefault(COMMENT_GENERAL, ACCELERATORMAX, "Accelerator max substance per one wheat");
        parser.getOrDefault(COMMENT_GENERAL, ACCELERATORRADIUS, "Accelerator working radius");
        parser.getOrDefault(COMMENT_GENERAL, ACCELERATORPERCOW, "Accelerator one substance per one cow");
        parser.getOrDefault(COMMENT_GENERAL, ACCELERATORMULTIPLIER, "Accelerator speed up multiplier");
        parser.getOrDefault(COMMENT_GENERAL, ACCELERATORWATER, "Accelerator water per one substance convert");

        parser.getOrDefault(COMMENT_GENERAL, BLACKLISTDIMIDS, "In what dim Id cow not spawn");
        parser.getOrDefault(COMMENT_GENERAL, ENABLECONVERTCOWTODISPLAYER, "If true u can convert cow into displayer via halter");
        parser.getOrDefault(COMMENT_GENERAL, BLACKLISTCOWTODISPLAYER, "Black list for 'cow to displayer' convert");
        parser.getOrDefault(COMMENT_GENERAL, FEEDERBLACKLIST, "Black list for 'what cows cant feed with Feeder'");

        parser.getOrDefault(COMMENT_GENERAL, EIOBLACKLISTSPAWNING, "EIO Powered Spawner cant spawn any cow");
        parser.getOrDefault(COMMENT_GENERAL, EIOBLACKLISTSOULVIAL, "EIO Soul Vial cant store any cow");
        parser.getOrDefault(COMMENT_GENERAL, EIONEEDSCLONING, "EIO Powered Spawner cloning cow every time (prevents spawn random cows from spawner)");
        parser.getOrDefault(COMMENT_GENERAL, EIOENTITYCOSTMULTIPLIER, "EIO Powered Spawner need multiplier energy cost to spawn cow");

        breedingItemWork = parser.getOrDefault(GENERAL, BREEDING, false);
        projecteTickRemove = parser.getOrDefault(GENERAL, PROJECTETICK, false);
        notenoughwandsTickRemove = parser.getOrDefault(GENERAL, NOTENOWANDSTICK, false);
        torcherinoTickRemove = parser.getOrDefault(GENERAL, TORCHERINOTICK, false);
        randomthingsTickRemove = parser.getOrDefault(GENERAL, RANDOMTHINGSTICK, false);
        disableBreedingItemForMachines = parser.getOrDefault(GENERAL, BREEDINGITEMMACHINES, false);

        spawnWeight = parser.getOrDefault(GENERAL, SPAWNWEIGHT, 8);
        spawnMin = parser.getOrDefault(GENERAL, SPAWNMIN, 4);
        spawnMax = parser.getOrDefault(GENERAL, SPAWNMAX, 4);
        spawnBlackListBiomes = parser.getOrDefault(GENERAL, SPAWNBLACKLIST, new String[] {"modid:name1", "modid:name2"});
        spawnBlackListBiomeKeys = Arrays.stream(spawnBlackListBiomes)
                .map(ResourceLocation::tryParse)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());

        acceleratorMax = parser.getOrDefault(GENERAL, ACCELERATORMAX, 6);
        acceleratorRadius = parser.getOrDefault(GENERAL, ACCELERATORRADIUS, 5);
        acceleratorPerCow = parser.getOrDefault(GENERAL, ACCELERATORPERCOW, 1);
        acceleratorMultiplier = parser.getOrDefault(GENERAL, ACCELERATORMULTIPLIER, 5);
        acceleratorWater = parser.getOrDefault(GENERAL, ACCELERATORWATER, 10);

        blackListDimIds = Arrays.stream(parser.getOrDefault(GENERAL, BLACKLISTDIMIDS, new int[0]))
                .boxed()
                .collect(Collectors.toUnmodifiableSet());
        enableConvertCowToDisplayer = parser.getOrDefault(GENERAL, ENABLECONVERTCOWTODISPLAYER, true);
        blackListCowToDisplayer = Arrays.stream(parser.getOrDefault(GENERAL, BLACKLISTCOWTODISPLAYER, new String[0]))
                .collect(Collectors.toUnmodifiableSet());
        feederBlackList = Arrays.stream(parser.getOrDefault(GENERAL, FEEDERBLACKLIST, new String[0]))
                .collect(Collectors.toUnmodifiableSet());

        EIOBlackListSpawning = parser.getOrDefault(GENERAL, EIOBLACKLISTSPAWNING, false);
        EIOBlackListSoulVial = parser.getOrDefault(GENERAL, EIOBLACKLISTSOULVIAL, false);
        EIONeedsCloning = parser.getOrDefault(GENERAL, EIONEEDSCLONING, true);
        EIOEntityCostMultiplier = parser.getOrDefault(GENERAL, EIOENTITYCOSTMULTIPLIER, 0);

        REGISTRY.clear();
        FLUIDS.clear();
        sumWeight = 0;
        breed.clear();
        canBreed.clear();

        for (Fluid fluid : FCUtils.getBucketFluids()) {
            ResourceLocation key = keyOf(fluid);
            if (key == null) {
                continue;
            }

            int rate = parser.getOrDefault(key.toString(), RATE, 100);
            boolean enable = parser.getOrDefault(key.toString(), ENABLE, true);
            int world = parser.getOrDefault(key.toString(), WORLD, 4000);
            int stall = parser.getOrDefault(key.toString(), STALL, 4000);
            int breedingChance = parser.getOrDefault(key.toString(), BREEDING_CHANCE, 50);
            int breedingCooldown = parser.getOrDefault(key.toString(), BREEDING_COOLDOWN, 6000);
            int growBaby = parser.getOrDefault(key.toString(), GROWING_BABY, -24000);
            FluidInfo info = new FluidInfo(rate, enable, world, stall, breedingChance, breedingCooldown, growBaby);
            REGISTRY.put(key, info);
            if (enable && rate > 0) {
                FLUIDS.add(fluid);
                sumWeight += rate;
            }
        }

        for (Fluid fluid : FCUtils.getBucketFluids()) {
            ResourceLocation key = keyOf(fluid);
            if (key == null || !isEnable(key)) {
                continue;
            }

            String parent1Name = parser.getOrDefault(key.toString(), PARENT_1, "");
            String parent2Name = parser.getOrDefault(key.toString(), PARENT_2, "");
            if (parent1Name.isEmpty() || parent2Name.isEmpty()) {
                continue;
            }

            Optional<ResourceLocation> parent1 = resolveFluidKey(parent1Name);
            Optional<ResourceLocation> parent2 = resolveFluidKey(parent2Name);
            if (parent1.isEmpty() || parent2.isEmpty()) {
                ModernFluidCows.LOGGER.warn(
                        "Breeding: Failed to add! First parent -> '{}'; Second parent -> '{}'; result -> '{}'",
                        parent1Name,
                        parent2Name,
                        key);
                continue;
            }

            if (!isEnable(parent1.get()) || !isEnable(parent2.get())) {
                continue;
            }

            CustomPair<ResourceLocation, ResourceLocation> pair = CustomPair.of(parent1.get(), parent2.get());
            List<ResourceLocation> list = breed.computeIfAbsent(pair, ignored -> new ArrayList<>());
            list.add(key);
            canBreed.add(key);
            ModernFluidCows.LOGGER.info(
                    "Breeding: Add new! First parent -> '{}'; Second parent -> '{}'; result -> '{}'",
                    parent1.get(),
                    parent2.get(),
                    key);
        }

        ModernFluidCows.LOGGER.info("Added {} breeding variants!", breed.size());

        parser.save();
        loaded = true;
    }

    /** Returns {@code true} when the supplied biome id is disallowed for natural spawns. */
    public static boolean isBiomeBlacklisted(final ResourceLocation key) {
        return spawnBlackListBiomeKeys.contains(key);
    }

    public static boolean isEnable(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).enable;
    }

    public static int getRate(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).rate;
    }

    public static int getWorldCD(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).world;
    }

    public static int getStallCD(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).stall;
    }

    public static int getChance(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).breedingChance;
    }

    public static int getBreedingCooldown(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).breedingCooldown;
    }

    public static int getGrowBaby(final ResourceLocation name) {
        return REGISTRY.getOrDefault(name, DEFAULT_FLUID).growBaby;
    }

    public static boolean canMateWith(final Fluid first, final Fluid second) {
        ResourceLocation left = keyOf(first);
        ResourceLocation right = keyOf(second);
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right) || breed.containsKey(CustomPair.of(left, right));
    }

    public static Fluid pickBreedingResult(final Fluid first, final Fluid second, final RandomSource random) {
        ResourceLocation left = keyOf(first);
        ResourceLocation right = keyOf(second);
        if (left == null || right == null) {
            return first;
        }
        if (left.equals(right)) {
            return first;
        }

        List<ResourceLocation> results = breed.get(CustomPair.of(left, right));
        if (results == null || results.isEmpty()) {
            return random.nextBoolean() ? first : second;
        }

        ResourceLocation chosenKey =
                results.size() == 1 ? results.getFirst() : results.get(random.nextInt(results.size()));
        Fluid chosen = BuiltInRegistries.FLUID.getOptional(chosenKey).orElse(null);
        if (chosen == null) {
            return random.nextBoolean() ? first : second;
        }

        if (random.nextInt(100) < getChance(chosenKey)) {
            return chosen;
        }
        return random.nextBoolean() ? first : second;
    }

    private static ResourceLocation keyOf(final Fluid fluid) {
        if (fluid == null) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
        if (key == null) {
            return null;
        }
        return key;
    }

    private static Optional<ResourceLocation> resolveFluidKey(final String name) {
        if (name.isEmpty()) {
            return Optional.empty();
        }
        ResourceLocation resource = ResourceLocation.tryParse(name);
        if (resource == null) {
            return Optional.empty();
        }
        if (!BuiltInRegistries.FLUID.containsKey(resource)) {
            return Optional.empty();
        }
        return Optional.of(resource);
    }

    private record FluidInfo(
            int rate,
            boolean enable,
            int world,
            int stall,
            int breedingChance,
            int breedingCooldown,
            int growBaby) {}
}
