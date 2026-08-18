package strhercules.chickens.network;

import strhercules.chickens.ChickensMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ChickensNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ChickensMod.MOD_ID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private ChickensNetwork() {
    }

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(id++, MegaChickenFlightPayload.class,
                MegaChickenFlightPayload::encode,
                MegaChickenFlightPayload::decode,
                MegaChickenFlightPayload::handle);
        CHANNEL.registerMessage(id++, SideConfigPayload.class,
                SideConfigPayload::encode,
                SideConfigPayload::decode,
                SideConfigPayload::handle);
    }
}
