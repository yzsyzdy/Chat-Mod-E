package com.niuqu.chatbubble.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C2S group management action from the client's [+] popup.
 * 0=create, 1=join, 2=leave, 3=delete. The server answers with a fresh
 * GroupListPayload; errors go back as plain system messages.
 */
public record GroupActionPayload(int action, String groupName) implements CustomPacketPayload {

    public static final int CREATE = 0;
    public static final int JOIN = 1;
    public static final int LEAVE = 2;
    public static final int DELETE = 3;

    private static final int MAX_NAME = 64;

    public static final CustomPacketPayload.Type<GroupActionPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "group_action"));

    public static final StreamCodec<FriendlyByteBuf, GroupActionPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeVarInt(value.action);
            buf.writeUtf(value.groupName, MAX_NAME);
        },
        buf -> new GroupActionPayload(buf.readVarInt(), buf.readUtf(MAX_NAME))
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** Client-side dispatch (callers guarantee a live connection). */
    public static void send(int action, String groupName) {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
            new GroupActionPayload(action, groupName));
    }
}
