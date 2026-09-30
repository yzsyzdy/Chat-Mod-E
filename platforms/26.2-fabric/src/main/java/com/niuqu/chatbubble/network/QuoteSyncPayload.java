package com.niuqu.chatbubble.network;

import Type;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record QuoteSyncPayload(String quotedSenderName, String quotedContent, String messageHash)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<QuoteSyncPayload> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("e33chat", "quote_sync"));

    public static final StreamCodec<FriendlyByteBuf, QuoteSyncPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeUtf(value.quotedSenderName);
            buf.writeUtf(value.quotedContent);
            buf.writeUtf(value.messageHash);
        },
        buf -> new QuoteSyncPayload(buf.readUtf(), buf.readUtf(), buf.readUtf())
    );

    @Override
    public Type<QuoteSyncPayload> type() { return ID; }
}
