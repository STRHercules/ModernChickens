package com.setycz.chickens.data;

import com.setycz.chickens.ChickensRegistryItem;
import com.setycz.chickens.SpawnType;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;

/**
 * Utility responsible for exporting the chicken breeding graph. The logic is
 * shared by both the bootstrap hook and the runtime command so the behaviour
 * stays consistent regardless of how the export is triggered.
 */
public final class BreedingGraphExporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreedingGraphExporter.class);

    private BreedingGraphExporter() {
    }

    /**
     * Writes the supplied chicken collection to {@code logs/chickens.gml}.
     *
     * @param chickens the registry snapshot to serialise.
     * @return the path that was written on success or {@link Optional#empty()}
     * when writing fails.
     */
    public static Optional<Path> export(Collection<ChickensRegistryItem> chickens) {
        Path logDir = FMLPaths.GAMEDIR.get().resolve("logs");
        Path output = logDir.resolve("chickens.gml");
        try {
            Files.createDirectories(logDir);
            try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                writer.write("graph [\n");
                writer.write("\tdirected 1\n");
                for (ChickensRegistryItem chicken : chickens) {
                    writer.write("\tnode [\n");
                    writer.write("\t\tid " + chicken.getId() + "\n");
                    writer.write("\t\tlabel \"" + chicken.getEntityName() + "\"\n");
                    if (requiresVisitingNether(chicken)) {
                        writer.write("\t\tgraphics [\n");
                        writer.write("\t\t\tfill \"#FF6600\"\n");
                        writer.write("\t\t]\n");
                    }
                    writer.write("\t]\n");
                }
                for (ChickensRegistryItem chicken : chickens) {
                    ChickensRegistryItem parent1 = chicken.getParent1();
                    ChickensRegistryItem parent2 = chicken.getParent2();
                    if (parent1 != null) {
                        writer.write("\tedge [\n");
                        writer.write("\t\tsource " + parent1.getId() + "\n");
                        writer.write("\t\ttarget " + chicken.getId() + "\n");
                        writer.write("\t]\n");
                    }
                    if (parent2 != null) {
                        writer.write("\tedge [\n");
                        writer.write("\t\tsource " + parent2.getId() + "\n");
                        writer.write("\t\ttarget " + chicken.getId() + "\n");
                        writer.write("\t]\n");
                    }
                }
                writer.write("]\n");
            }
            LOGGER.info("Wrote chicken breeding graph to {}", output);
            return Optional.of(output);
        } catch (IOException e) {
            LOGGER.warn("Failed to write chicken breeding graph", e);
            return Optional.empty();
        }
    }

    private static boolean requiresVisitingNether(ChickensRegistryItem chicken) {
        if (chicken == null) {
            return false;
        }
        if (!chicken.isBreedable()) {
            return chicken.getSpawnType() == SpawnType.HELL;
        }
        return requiresVisitingNether(chicken.getParent1()) || requiresVisitingNether(chicken.getParent2());
    }
}

