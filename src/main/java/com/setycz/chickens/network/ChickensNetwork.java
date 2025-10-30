package com.setycz.chickens.network;

import com.setycz.chickens.ChickensMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Wires the mod's custom network payloads into NeoForge's registration event so
 * both the server and client agree on stream codecs and protocol versions.
 */
public final class ChickensNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ChickensNetwork() {
    }

    /**
     * Registers the payload handler hook with the mod event bus.
     */
    public static void init(IEventBus modBus) {
        modBus.addListener(ChickensNetwork::onRegisterPayloadHandlers);
    }

    private static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ChickensMod.MOD_ID).versioned(PROTOCOL_VERSION);
        registrar.playToClient(CollectorDebugTogglePayload.TYPE, CollectorDebugTogglePayload.STREAM_CODEC,
                CollectorDebugTogglePayload::handle);
    }
}
