package com.setycz.chickens.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.setycz.chickens.ChickensRegistry;
import com.setycz.chickens.data.BreedingGraphExporter;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
                                .executes(ctx -> exportBreedingGraph(ctx.getSource()))));
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
}

