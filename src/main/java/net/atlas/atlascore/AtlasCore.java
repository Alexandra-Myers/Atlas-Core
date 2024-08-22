package net.atlas.atlascore;

import net.atlas.atlascore.command.ConfigCommand;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasCoreConfig;
import net.atlas.atlascore.init.ArgumentInit;
import net.atlas.atlascore.util.PrefixLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

public class AtlasCore implements ModInitializer {
    public static AtlasCoreConfig CONFIG = new AtlasCoreConfig();
    public static ResourceLocation modDetectionNetworkChannel = id("networking");
    public static final String MOD_ID = "atlas-core";
    public static final PrefixLogger LOGGER = new PrefixLogger(LogManager.getLogger("Atlas Core"));
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playS2C().register(AtlasConfigPacket.TYPE, AtlasConfigPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register(modDetectionNetworkChannel,(handler, sender, server) -> {
            for (AtlasConfig atlasConfig : AtlasConfig.configs.values()) {
                ServerPlayNetworking.send(handler.player, new AtlasConfigPacket(false, atlasConfig));
            }
            AtlasCore.LOGGER.info("Config packets sent to client.");
        });
        ArgumentInit.registerArguments();
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ConfigCommand.register(dispatcher));
    }
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public record AtlasConfigPacket(boolean forCommand, AtlasConfig config) implements CustomPacketPayload {
        public static final Type<AtlasConfigPacket> TYPE = new Type<>(id("atlas_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, AtlasConfigPacket> CODEC = CustomPacketPayload.codec(AtlasConfigPacket::write, AtlasConfigPacket::new);

        public AtlasConfigPacket(RegistryFriendlyByteBuf buf) {
            this(buf.readBoolean(), AtlasConfig.staticLoadFromNetwork(buf));
        }

        public void write(RegistryFriendlyByteBuf buf) {
            buf.writeBoolean(forCommand);
            buf.writeResourceLocation(config.name);
            config.saveToNetwork(buf);
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
