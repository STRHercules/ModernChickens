package com.setycz.chickens.integration.kubejs;

import dev.latvian.mods.rhino.util.RemapPrefixForJS;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class ChickenRegistryType {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ChickenRegistryType.class);
    
    /**
     * Wrapper class that provides the ChickensEvents API to JavaScript
     * Usage in JS: ChickensEvents.registry(event => { ... })
     */
    @RemapPrefixForJS("chickensEvents$")
    public static class ChickensEventsWrapper {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(ChickensEventsWrapper.class);
        
        /**
         * Called from JavaScript to register chickens
         * @param callback JavaScript function that receives the event
         */
        public static void chickensEvents$registry(Consumer<ChickenRegistryEventJS> callback) {
            LOGGER.info("ChickensEvents.registry called from KubeJS script");
            
            ChickenRegistryEventJS event = new ChickenRegistryEventJS();
            
            try {
                callback.accept(event);
                LOGGER.info("Chicken registration callback executed successfully");
                
                // Build all pending chickens
                event.buildAll();
                
                // After all chickens are registered, resolve parent relationships
                event.afterPosted();
            } catch (Exception e) {
                LOGGER.error("Error executing chicken registration callback", e);
                throw new RuntimeException("Failed to register chickens", e);
            }
        }
    }
    
    /**
     * Event handler class for registering chickens from KubeJS scripts
     * This is exposed to JavaScript through ChickensEvents.registry(event => {...})
     */
    public static class ChickenRegistryEventJS {
        
        private static final Logger LOGGER = LoggerFactory.getLogger(ChickenRegistryEventJS.class);
        private static boolean parentsResolved = false;
        private final List<ChickenBuilder> pendingBuilders = new ArrayList<>();
        
        /**
         * Creates a new chicken builder
         * @param id The chicken ID (e.g., "dirt_chicken")
         * @return A new ChickenBuilder instance
         */
        public ChickenBuilder create(String id) {
            ResourceLocation resourceLocation;
            
            // Handle both formats: "dirt_chicken" and "chickens:dirt_chicken"
            if (id.contains(":")) {
                resourceLocation = ResourceLocation.parse(id);
            } else {
                resourceLocation = ResourceLocation.fromNamespaceAndPath("chickens", id);
            }
            
            LOGGER.info("Creating chicken builder for: {}", resourceLocation);
            ChickenBuilder builder = new ChickenBuilder(resourceLocation);
            pendingBuilders.add(builder);
            return builder;
        }
        
        /**
         * Builds all pending chickens
         */
        public void buildAll() {
            LOGGER.info("Building {} chickens...", pendingBuilders.size());
            for (ChickenBuilder builder : pendingBuilders) {
                try {
                    builder.build();
                } catch (Exception e) {
                    LOGGER.error("Failed to build chicken {}", builder.getId(), e);
                    throw e;
                }
            }
            LOGGER.info("Successfully built {} chickens", pendingBuilders.size());
        }
        
        /**
         * Called after all registry events complete
         * Resolves parent relationships for all registered chickens
         */
        public void afterPosted() {
            if (!parentsResolved) {
                ChickenBuilder.resolveAllParents();
                parentsResolved = true;
                LOGGER.info("ChickenRegistryEventJS: Parent relationships resolved for {} chickens", pendingBuilders.size());
            }
        }
    }
}