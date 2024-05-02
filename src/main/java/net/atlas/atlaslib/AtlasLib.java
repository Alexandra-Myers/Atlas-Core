package net.atlas.atlaslib;

import net.atlas.atlaslib.config.AtlasConfig;
import net.atlas.atlaslib.util.PrefixLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

public class AtlasLib implements ModInitializer {
    public static ResourceLocation modDetectionNetworkChannel = id("networking");
    public static final String MOD_ID = "atlas-lib";
    public static final PrefixLogger LOGGER = new PrefixLogger(LogManager.getLogger("Atlas Lib"));
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(AtlasConfigPacket.TYPE, AtlasConfigPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register(modDetectionNetworkChannel,(handler, sender, server) -> {
            for (AtlasConfig atlasConfig : AtlasConfig.configs.values()) {
                ServerPlayNetworking.send(handler.player, new AtlasConfigPacket(atlasConfig));
            }
            AtlasLib.LOGGER.info("Config packets sent to client.");
        });
    }
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public record AtlasConfigPacket(AtlasConfig config) implements CustomPacketPayload {
        public static final Type<AtlasConfigPacket> TYPE = CustomPacketPayload.createType(id("atlas_config").toString());
        public static final StreamCodec<RegistryFriendlyByteBuf, AtlasConfigPacket> CODEC = CustomPacketPayload.codec(AtlasConfigPacket::write, AtlasConfigPacket::new);

        public AtlasConfigPacket(RegistryFriendlyByteBuf buf) {
            this(AtlasConfig.staticLoadFromNetwork(buf));
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeResourceLocation(config.name);
            config.saveToNetwork(buf);
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
