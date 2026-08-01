package strhercules.chickens.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import strhercules.chickens.ChickensRegistry;
import strhercules.chickens.data.BreedingGraphExporter;
import strhercules.chickens.ChickensRegistryItem;
import strhercules.chickens.SpawnType;
import strhercules.chickens.entity.ChickensChicken;
import strhercules.chickens.registry.ModEntityTypes;
import strhercules.chickens.registry.ModRegistry;
import strhercules.chickens.spawn.ChickensSpawnDebug;
import strhercules.chickens.spawn.ChickensSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Locale;


/**
 * Registers the Chickens command tree. Modern NeoForge exposes Brigadier
 * directly, so we expose a {@code /chickens export breeding} command that lets
 * players regenerate the breeding graph without restarting the server.
 */
public final class ChickensCommands {
    private ChickensCommands() {
    }

    public static void init() {
        // Listen for the command registration callback on the Forge event bus.
        NeoForge.EVENT_BUS.addListener(ChickensCommands::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chickens")
                .requires(source -> source.hasPermission(4))
                .then(Commands.literal("export")
                        .then(Commands.literal("breeding")
                                .executes(ctx -> exportBreedingGraph(ctx.getSource()))))
                .then(Commands.literal("kill")
                        .executes(ctx -> killAllChickens(ctx.getSource())))
                .then(Commands.literal("custom")
                        .then(Commands.literal("list")
                                .executes(ctx -> listCustomChickens(ctx.getSource()))))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("multiplier")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 1000.0F))
                                .executes(ctx -> setSpawnMultiplier(ctx.getSource(), FloatArgumentType.getFloat(ctx, "value")))))
                        .then(Commands.literal("debug")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> toggleSpawnDebug(ctx.getSource(), BoolArgumentType.getBool(ctx, "enabled")))))
                        .then(Commands.literal("summon")
                                .then(Commands.argument("chicken", StringArgumentType.greedyString())
                                        .executes(ctx -> summonSpecific(ctx.getSource(), StringArgumentType.getString(ctx, "chicken")))))
                        .then(Commands.literal("summon_random")
                                .executes(ctx -> summonRandom(ctx.getSource(), null))
                                .then(Commands.argument("spawn_type", StringArgumentType.word())
                                        .executes(ctx -> summonRandom(ctx.getSource(), StringArgumentType.getString(ctx, "spawn_type"))))))
                .then(Commands.literal("givechickenitem")
                        .then(Commands.argument("chicken", StringArgumentType.word())
                                .then(Commands.argument("growth", IntegerArgumentType.integer(1, 10))
                                        .then(Commands.argument("gain", IntegerArgumentType.integer(1, 10))
                                                .then(Commands.argument("strength", IntegerArgumentType.integer(1, 10))
                                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 16))
                                                                .executes(ctx -> giveChickenItem(
                                                                        ctx.getSource(),
                                                                        StringArgumentType.getString(ctx, "chicken"),
                                                                        IntegerArgumentType.getInteger(ctx, "growth"),
                                                                        IntegerArgumentType.getInteger(ctx, "gain"),
                                                                        IntegerArgumentType.getInteger(ctx, "strength"),
                                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))));
        event.getDispatcher().register(root);
    }

    private static int listCustomChickens(CommandSourceStack source) {
        var custom = ChickensRegistry.getItems().stream().filter(ChickensRegistryItem::isCustom).toList();
        if (custom.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No custom chickens are loaded."), false);
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Loaded custom chickens: " + custom.size()), false);
        for (ChickensRegistryItem chicken : custom) {
            source.sendSuccess(() -> Component.literal(chicken.getId() + ": " + chicken.getEntityName()), false);
        }
        return custom.size();
    }

    // ---------------------------------------------------------------------------
    // /chickens givechickenitem <chickenId> <growth> <gain> <strength> <amount>
    //
    // Each stat is already validated to [1,10] by Brigadier's IntegerArgumentType.
    // chickenId accepts: numeric id, entity name (with or without underscores),
    // or display name — all case-insensitive.
    // ---------------------------------------------------------------------------
    private static int giveChickenItem(CommandSourceStack source, String token,
                                       int growth, int gain, int strength, int amount) {
        // --- resolve the chicken type ---
        ChickensRegistryItem chicken = resolveChicken(token);
        if (chicken == null) {
            source.sendFailure(Component.literal("Unknown chicken: \"" + token + "\"."));
            return 0;
        }

        // --- resolve executing player ---
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (Exception e) {
            source.sendFailure(Component.literal("This command must be run by a player."));
            return 0;
        }

        // --- build the ItemStack ---
        // ModRegistry.CHICKEN_ITEM is the "chickens:chicken" item (ChickenItem) —
        // the same one KubeJS reports with custom_model_data + custom_data NBTs.
        ItemStack stack = new ItemStack(ModRegistry.CHICKEN_ITEM.get(), amount);

        // custom_model_data — mirrors what KubeJS shows (e.g. 105 for chickenType 105)
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(chicken.getId()));

        // custom_data  — ChickenStats compound + ChickenType int
        CompoundTag chickenData = new CompoundTag();
        CompoundTag chickenStats = new CompoundTag();
        chickenStats.putByte("Analyzed", (byte) 0);
        chickenStats.putInt("Gain",     gain);
        chickenStats.putInt("Growth",   growth);
        chickenStats.putInt("Strength", strength);
        chickenData.put("ChickenStats", chickenStats);
        chickenData.putInt("ChickenType", chicken.getId());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(chickenData));

        // --- give to player (handle overflow into world) ---
        boolean fullyGiven = player.getInventory().add(stack);
        if (!fullyGiven && !stack.isEmpty()) {
            player.drop(stack, false);
        }

        final String displayName = chicken.getDisplayName().getString();
        source.sendSuccess(() -> Component.literal(
                String.format("Gave %dx %s (Growth:%d Gain:%d Strength:%d) to %s",
                        amount, displayName, growth, gain, strength, player.getName().getString())),
                true);
        return amount;
    }

    // ---------------------------------------------------------------------------
    // existing handlers (unchanged)
    // ---------------------------------------------------------------------------

    private static int exportBreedingGraph(CommandSourceStack source) {
        Optional<Path> result = BreedingGraphExporter.export(ChickensRegistry.getItems());
        if (result.isPresent()) {
            Path path = result.get();
            source.sendSuccess(() -> Component.translatable("commands.chickens.export.success", path.toString()), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.chickens.export.failure"));
        return 0;
    }

    private static int setSpawnMultiplier(CommandSourceStack source, float multiplier) {
        ChickensSpawnDebug.setSpawnWeightMultiplier(multiplier);
        String formatted = String.format(Locale.ROOT, "%.2f", multiplier);
        source.sendSuccess(() -> Component.translatable("commands.chickens.spawn.multiplier", formatted), true);
        return 1;
    }

    private static int toggleSpawnDebug(CommandSourceStack source, boolean enabled) {
        ChickensSpawnDebug.setLoggingEnabled(enabled);
        Component state = enabled ? Component.translatable("options.on") : Component.translatable("options.off");
        source.sendSuccess(() -> Component.translatable("commands.chickens.spawn.debug", state), true);
        return 1;
    }

    private static int summonSpecific(CommandSourceStack source, String token) {
        ChickensRegistryItem chicken = resolveChicken(token);
        if (chicken == null) {
            source.sendFailure(Component.translatable("commands.chickens.spawn.unknown", token));
            return 0;
        }
        if (spawnChickenNearSource(source, chicken)) {
            source.sendSuccess(() -> Component.translatable("commands.chickens.spawn.success", chicken.getDisplayName()), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.chickens.spawn.failure"));
        return 0;
    }

    private static int summonRandom(CommandSourceStack source, String spawnTypeToken) {
        SpawnType type = spawnTypeToken == null ? SpawnType.NORMAL : parseSpawnType(spawnTypeToken);
        if (type == null) {
            source.sendFailure(Component.translatable("commands.chickens.spawn.unknown_type", spawnTypeToken));
            return 0;
        }
        Optional<ChickensRegistryItem> result = ChickensSpawnManager.pickChicken(type, source.getLevel().getRandom());
        if (result.isEmpty()) {
            source.sendFailure(Component.translatable("commands.chickens.spawn.none_available", type.name().toLowerCase(Locale.ROOT)));
            return 0;
        }
        if (spawnChickenNearSource(source, result.get())) {
            source.sendSuccess(() -> Component.translatable("commands.chickens.spawn.success", result.get().getDisplayName()), true);
            return 1;
        }
        source.sendFailure(Component.translatable("commands.chickens.spawn.failure"));
        return 0;
    }

    private static boolean spawnChickenNearSource(CommandSourceStack source, ChickensRegistryItem chicken) {
        ServerLevel level = source.getLevel();
        ChickensChicken entity = ModEntityTypes.CHICKENS_CHICKEN.get().create(level);
        if (entity == null) {
            return false;
        }
        entity.setChickenType(chicken.getId());
        var pos = source.getPosition();
        var rot = source.getRotation();
        entity.moveTo(pos.x, pos.y, pos.z, rot.y, rot.x);
        entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.COMMAND, null);
        level.addFreshEntity(entity);
        return true;
    }

    private static int killAllChickens(CommandSourceStack source) {
        ServerLevel level = source.getLevel();
        java.util.List<ChickensChicken> all = new java.util.ArrayList<>();
        level.getAllEntities().forEach(e -> {
            if (e instanceof ChickensChicken c) all.add(c);
        });
        int count = 0;
        for (ChickensChicken chicken : all) {
            chicken.discard();
            count++;
        }
        final int total = count;
        if (total == 0) {
            source.sendSuccess(() -> Component.translatable("commands.chickens.kill.none"), true);
        } else {
            source.sendSuccess(() -> Component.translatable("commands.chickens.kill.success", total), true);
        }
        return total;
    }

    private static ChickensRegistryItem resolveChicken(String token) {
        token = token.trim();
        if (token.isEmpty()) {
            return null;
        }
        // 1. numeric id
        try {
            int id = Integer.parseInt(token);
            return ChickensRegistry.getByType(id);
        } catch (NumberFormatException ignored) {
        }
        // 2. exact / case-insensitive match on entityName or displayName
        String lower = token.toLowerCase(Locale.ROOT);
        // also try normalizing: strip underscores for comparison (netherithechicken == netherite_chicken)
        String lowerNoUnderscore = lower.replace("_", "");
        for (ChickensRegistryItem item : ChickensRegistry.getItems()) {
            String entityLower   = item.getEntityName().toLowerCase(Locale.ROOT);
            String displayLower  = item.getDisplayName().getString().toLowerCase(Locale.ROOT);
            if (entityLower.equals(lower) || displayLower.equals(lower)) {
                return item;
            }
            // underscore-stripped fallback: lets "netherithechicken" match "netherite_chicken"
            if (entityLower.replace("_", "").equals(lowerNoUnderscore)
                    || displayLower.replace(" ", "").replace("_", "").equals(lowerNoUnderscore)) {
                return item;
            }
        }
        return null;
    }

    private static SpawnType parseSpawnType(String token) {
        try {
            return SpawnType.valueOf(token.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
