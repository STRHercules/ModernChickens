package com.setycz.chickens.command;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.data.BreedingGraphExporter;
import com.setycz.chickens.debug.CollectorDebugState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Path;
import java.util.Optional;

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
                .then(Commands.literal("debug")
                        .then(Commands.literal("collector_range")
                                .executes(ctx -> toggleCollectorDebug(ctx.getSource()))
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> setCollectorDebug(ctx.getSource(),
                                                BoolArgumentType.getBool(ctx, "enabled"))))));
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

    private static int toggleCollectorDebug(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        boolean enabled = CollectorDebugState.toggle(player);
        source.sendSuccess(() -> Component.translatable(enabled
                ? "commands.chickens.debug.collector_range.enabled"
                : "commands.chickens.debug.collector_range.disabled"), true);
        return enabled ? 1 : 0;
    }

    private static int setCollectorDebug(CommandSourceStack source, boolean enabled) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        CollectorDebugState.set(player, enabled);
        source.sendSuccess(() -> Component.translatable(enabled
                ? "commands.chickens.debug.collector_range.enabled"
                : "commands.chickens.debug.collector_range.disabled"), true);
        return enabled ? 1 : 0;
    }
}

