package com.niuqu.chatbubble.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * C2S handshake: the client announces it runs E33Chat right after logging in.
 * The server uses this to route group chat (packet for mod clients, plain
 * formatted line for vanilla ones) and to push the group list immediately.
 */
public record ClientHelloPayload() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClientHelloPayload> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("e33chat", "client_hello"));

    public static final StreamCodec<FriendlyByteBuf, ClientHelloPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {},
        buf -> new ClientHelloPayload()
    );

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** Client-side dispatch (callers guarantee a live connection). */
    public static void send() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(new ClientHelloPayload());
    }

    /** Server-side hook, invoked from ChatBubbleMod's receiver. */
    public static void handleServer(ClientHelloPayload payload, ServerPlayer player) {
        com.niuqu.chatbubble.server.GroupManager.onClientHello(player);
    }
}
