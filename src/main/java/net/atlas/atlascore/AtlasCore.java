package net.atlas.atlascore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.atlas.atlascore.command.ConfigCommand;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasCoreConfig;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.atlas.atlascore.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.function.Consumer;

public class AtlasCore implements ModInitializer {
    public static final String MOD_ID = "atlas-core";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final PrefixLogger LOGGER = new PrefixLogger(LogManager.getLogger("Atlas Core"));
    public static AtlasCoreConfig CONFIG;
    public static ResourceLocation modDetectionNetworkChannel = id("networking");
    /**
     * Runs the mod initializer.
     */
    @Override
    public void onInitialize() {
        CONFIG = new AtlasCoreConfig();
        PayloadTypeRegistry.playS2C().register(AtlasConfigPacket.TYPE, AtlasConfigPacket.CODEC);
        PayloadTypeRegistry.playC2S().register(ClientInformPacket.TYPE, ClientInformPacket.CODEC);
        PayloadTypeRegistry.configurationC2S().register(ServerboundClientModPacket.TYPE, ServerboundClientModPacket.CODEC);
        PayloadTypeRegistry.configurationS2C().register(ClientboundModListRetrievalPacket.TYPE, ClientboundModListRetrievalPacket.CODEC);
        ServerPlayConnectionEvents.JOIN.register(modDetectionNetworkChannel,(handler, sender, server) -> {
            for (AtlasConfig atlasConfig : AtlasConfig.configs.values().stream().filter(atlasConfig -> atlasConfig.configSide.isCommon()).toList()) {
                if (atlasConfig instanceof ContextBasedConfig contextBasedConfig) atlasConfig = contextBasedConfig.getConfig(Context.builder().applyInformationFromEntity(handler.player).build());
                ServerPlayNetworking.send(handler.player, new AtlasConfigPacket(false, atlasConfig));
            }
            AtlasCore.LOGGER.info("Config packets sent to client.");
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(modDetectionNetworkChannel, (player, origin, destination) -> {
            for (ContextBasedConfig contextBasedConfig : AtlasConfig.configs.values().stream().filter(atlasConfig -> atlasConfig.configSide.isCommon() && atlasConfig instanceof ContextBasedConfig).map(config -> ((ContextBasedConfig) config).getConfig(Context.builder().applyInformationFromLevel(destination).build())).toList()) {
                ServerPlayNetworking.send(player, new AtlasConfigPacket(false, contextBasedConfig));
            }
        });
        ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
            if (ServerConfigurationNetworking.canSend(handler, ClientboundModListRetrievalPacket.TYPE))
                handler.addTask(new ClientModRetrievalTask());
        });
        ServerConfigurationNetworking.registerGlobalReceiver(ServerboundClientModPacket.TYPE, (payload, context) -> {
            ServerModsRetrievedEvent.RETRIEVAL.invoker().onModsReceived(context.networkHandler(), context.responseSender(), payload.modRepresentations());
            context.networkHandler().completeTask(ClientModRetrievalTask.TYPE);
        });
        ServerPlayNetworking.registerGlobalReceiver(AtlasCore.ClientInformPacket.TYPE, (packet, context) -> packet.config().handleConfigInformation(packet, context.player(), context.responseSender()));
        ServerModsRetrievedEvent.RETRIEVAL.register((handler, sender, mods) -> {
            if (CONFIG.listClientModsOnJoin.get()) {
                final String[] list = {"Client mods: \n"};
                mods.stream().sorted(Comparator.comparing(ModRepresentation::modID)).forEach(modRepresentation -> modRepresentation.list(string -> list[0] = list[0] + string + "\n", ModRepresentation::modID, "\t» | "));
                LOGGER.info(list[0].substring(0, list[0].length() - 1));
            }
        });
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
    public record ClientInformPacket(AtlasConfig config) implements CustomPacketPayload {
        public static final Type<ClientInformPacket> TYPE = new Type<>(id("c2s_inform_config"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ClientInformPacket> CODEC = CustomPacketPayload.codec(ClientInformPacket::write, ClientInformPacket::new);

        public ClientInformPacket(RegistryFriendlyByteBuf buf) {
            this(AtlasConfig.staticReadClientConfigInformation(buf));
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

    public record ClientModRetrievalTask() implements ConfigurationTask {
        public static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(id("client_mods_retrieval").toString());

        @Override
        public void start(Consumer<Packet<?>> sender) {
            sender.accept(ServerConfigurationNetworking.createS2CPacket(new ClientboundModListRetrievalPacket()));
        }

        @Override
        public @NotNull Type type() {
            return TYPE;
        }
    }

    @SuppressWarnings("unused")
    public record ClientboundModListRetrievalPacket() implements CustomPacketPayload {
        public static final Type<ClientboundModListRetrievalPacket> TYPE = new Type<>(id("client_mods_retrieval"));
        public static final StreamCodec<FriendlyByteBuf, ClientboundModListRetrievalPacket> CODEC = CustomPacketPayload.codec(ClientboundModListRetrievalPacket::write, ClientboundModListRetrievalPacket::new);

        public ClientboundModListRetrievalPacket(FriendlyByteBuf buf) {
            this();
        }

        public void write(FriendlyByteBuf buf) {

        }

        /**
         * Returns the packet type of this packet.
         *
         * <p>Implementations should store the packet type instance in a {@code static final}
         * field and return that here, instead of creating a new instance.
         *
         * @return the type of this packet
         */
        @Override
        public @NotNull Type<?> type() {
            return TYPE;
        }
    }

    public record ServerboundClientModPacket(Collection<ModRepresentation> modRepresentations) implements CustomPacketPayload {
        public static final Type<ServerboundClientModPacket> TYPE = new Type<>(id("client_mods"));
        public static final StreamCodec<FriendlyByteBuf, ServerboundClientModPacket> CODEC = CustomPacketPayload.codec(ServerboundClientModPacket::write, ServerboundClientModPacket::new);

        public ServerboundClientModPacket(FriendlyByteBuf buf) {
            this(NetworkingUtilities.modsFromNetwork(buf));
        }

        public void write(FriendlyByteBuf buf) {
            NetworkingUtilities.modsToNetwork(buf, modRepresentations);
        }

        /**
         * Returns the packet type of this packet.
         *
         * <p>Implementations should store the packet type instance in a {@code static final}
         * field and return that here, instead of creating a new instance.
         *
         * @return the type of this packet
         */
        @Override
        public @NotNull Type<?> type() {
            return TYPE;
        }
    }
}
