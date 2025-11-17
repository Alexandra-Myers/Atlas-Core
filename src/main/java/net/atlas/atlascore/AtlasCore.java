package net.atlas.atlascore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.atlas.atlascore.client.AtlasCoreClient;
import net.atlas.atlascore.command.ConfigCommand;
import net.atlas.atlascore.config.AtlasConfig;
import net.atlas.atlascore.config.AtlasCoreConfig;
import net.atlas.atlascore.config.ContextBasedConfig;
import net.atlas.atlascore.util.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;

@Mod(value = AtlasCore.MOD_ID)
@Mod.EventBusSubscriber(modid = AtlasCore.MOD_ID)
public class AtlasCore {
    public static final String MOD_ID = "atlas_core";
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final PrefixLogger LOGGER = new PrefixLogger(LogManager.getLogger("Atlas Core"));
    public static AtlasCoreConfig CONFIG;
    public static ResourceLocation modDetectionNetworkChannel = id("networking");
    public AtlasCore(FMLJavaModLoadingContext context) {
        CONFIG = new AtlasCoreConfig();
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
        ServerPlayNetworking.registerGlobalReceiver(ServerboundClientModPacket.TYPE, (payload, player, responseSender) -> {
            ServerModsRetrievedEvent.RETRIEVAL.invoker().onModsReceived(player.connection, responseSender, payload.modRepresentations());
        });
        ServerPlayNetworking.registerGlobalReceiver(ClientInformPacket.TYPE, (packet, player, responseSender) -> packet.config().handleConfigInformation(packet, player, responseSender));
        ServerModsRetrievedEvent.RETRIEVAL.register((handler, sender, mods) -> {
            if (CONFIG.listClientModsOnJoin.get()) {
                final String[] list = {"Client mods: \n"};
                mods.stream().sorted(Comparator.comparing(ModRepresentation::modID)).forEach(modRepresentation -> modRepresentation.list(string -> list[0] = list[0] + string + "\n", ModRepresentation::modID, "\t» | "));
                LOGGER.info(list[0].substring(0, list[0].length() - 1));
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> ConfigCommand.register(dispatcher));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AtlasCoreClient.load(context));
    }
    public static ResourceLocation id(String path) {
        //noinspection removal
        return new ResourceLocation(MOD_ID, path);
    }

    public record AtlasConfigPacket(boolean forCommand, AtlasConfig config) implements FabricPacket {
        public static final PacketType<AtlasConfigPacket> TYPE = PacketType.create(id("atlas_config"), AtlasConfigPacket::new);
        
        public AtlasConfigPacket(FriendlyByteBuf buf) {
            this(buf.readBoolean(), AtlasConfig.staticLoadFromNetwork(buf));
        }

        @Override
        public void write(FriendlyByteBuf buf) {
            buf.writeBoolean(forCommand);
            buf.writeResourceLocation(config.name);
            config.saveToNetwork(buf);
        }

        @Override
        public @NotNull PacketType<? extends FabricPacket> getType() {
            return TYPE;
        }
    }
    public record ClientInformPacket(AtlasConfig config) implements FabricPacket {
        public static final PacketType<ClientInformPacket> TYPE = PacketType.create(id("c2s_inform_config"), ClientInformPacket::new);

        public ClientInformPacket(FriendlyByteBuf buf) {
            this(AtlasConfig.staticReadClientConfigInformation(buf));
        }

        public void write(FriendlyByteBuf buf) {
            buf.writeResourceLocation(config.name);
            config.saveToNetwork(buf);
        }

        @Override
        public @NotNull PacketType<? extends FabricPacket> getType() {
            return TYPE;
        }
    }

    @SuppressWarnings("unused")
    public record ClientboundModListRetrievalPacket() implements FabricPacket {
        public static final PacketType<ClientboundModListRetrievalPacket> TYPE = PacketType.create(id("client_mods_retrieval"), ClientboundModListRetrievalPacket::new);

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
        public @NotNull PacketType<?> getType() {
            return TYPE;
        }
    }

    public record ServerboundClientModPacket(Collection<ModRepresentation> modRepresentations) implements FabricPacket {
        public static final PacketType<ServerboundClientModPacket> TYPE = PacketType.create(id("client_mods"), ServerboundClientModPacket::new);

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
        public @NotNull PacketType<?> getType() {
            return TYPE;
        }
    }
}
