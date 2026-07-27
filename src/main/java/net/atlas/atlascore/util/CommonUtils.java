package net.atlas.atlascore.util;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.mehvahdjukaar.codecui.SchemaContext;
import net.minecraft.commands.CommandSourceStack;
//? >=1.21.5 {
import net.minecraft.nbt.NbtOps;
//?}
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? >=1.21.11 {
import net.minecraft.server.permissions.Permissions;
//?}

public class CommonUtils {
    public static void writeId(FriendlyByteBuf buf, Identifier id) {
        buf.writeIdentifier(id);
    }
    public static Identifier readId(FriendlyByteBuf buf) {
        return buf.readIdentifier();
    }

    public static Packet<ClientCommonPacketListener> createClientboundConfigurationPacket(CustomPacketPayload payload) {
        //? >=26.1 {
        return ServerConfigurationNetworking.createClientboundPacket(payload);
        //?}
        //? <26.1 {
        /*return ServerConfigurationNetworking.createS2CPacket(payload);
        *///?}
    }

    public static Packet<ClientCommonPacketListener> createClientboundPlayPacket(CustomPacketPayload payload) {
        //? >=26.1 {
        return ServerPlayNetworking.createClientboundPacket(payload);
         //?}
        //? <26.1 {
        /*return ServerPlayNetworking.createS2CPacket(payload);
        *///?}
    }

    public static Packet<ServerCommonPacketListener> createServerboundPlayPacket(CustomPacketPayload payload) {
        //? >=26.1 {
        return ClientPlayNetworking.createServerboundPacket(payload);
         //?}
        //? <26.1 {
        /*return ClientPlayNetworking.createC2SPacket(payload);
        *///?}
    }

    public static Tag read(StringReader reader) throws CommandSyntaxException {
        //? >=1.21.5 {
        return TagParser.create(SchemaContext.getRegistries().createSerializationContext(NbtOps.INSTANCE)).parseAsArgument(reader);
        //?}
        //? <1.21.5 {
        /*return new TagParser(reader).readValue();
        *///?}
    }

    public static boolean hasPerms(CommandSourceStack commandSourceStack) {
        //? >=1.21.11 {
        return commandSourceStack.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        //?}
        //? <1.21.11 {
        /*return commandSourceStack.hasPermission(2);
        *///?}
    }
}
