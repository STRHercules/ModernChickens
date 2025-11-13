package com.setycz.chickens.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.data.BreedingGraphExporter;
import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import com.setycz.chickens.entity.ChickensChicken;
import com.setycz.chickens.registry.ModEntityTypes;
import com.setycz.chickens.spawn.ChickensSpawnDebug;
import com.setycz.chickens.spawn.ChickensSpawnManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
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
        // Commands.literal returns a LiteralArgumentBuilder; keeping a local
        // variable helps readability while constructing the command tree.
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("chickens")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("export")
                        .then(Commands.literal("breeding")
                                .executes(ctx -> exportBreedingGraph(ctx.getSource()))))
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
                                        .executes(ctx -> summonRandom(ctx.getSource(), StringArgumentType.getString(ctx, "spawn_type"))))));
        event.getDispatcher().register(root);
    }

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

    private static ChickensRegistryItem resolveChicken(String token) {
        token = token.trim();
        if (token.isEmpty()) {
            return null;
        }
        try {
            int id = Integer.parseInt(token);
            return ChickensRegistry.getByType(id);
        } catch (NumberFormatException ignored) {
        }
        String lower = token.toLowerCase(Locale.ROOT);
        for (ChickensRegistryItem item : ChickensRegistry.getItems()) {
            if (item.getEntityName().equalsIgnoreCase(token) || item.getDisplayName().getString().equalsIgnoreCase(token)) {
                return item;
            }
            if (lower.equals(item.getDisplayName().getString().toLowerCase(Locale.ROOT))) {
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
