package com.niuqu.chatbubble.network;

import Type;
import com.niuqu.chatbubble.store.ChatMessageStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server -> client: EasyBot compatibility toggle (2.4.3-beta).
 *
 * A separate payload type on purpose: old clients drop unknown payloads
 * harmlessly, so a mixed-version client/server never desyncs. Absent payload =
 * disabled, matching the server config default.
 */
public record EasyBotConfigPayload(boolean easyBotCompat) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<EasyBotConfigPayload> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("e33chat", "config_sync_easybot"));

    public static final StreamCodec<FriendlyByteBuf, EasyBotConfigPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> buf.writeBoolean(value.easyBotCompat),
        buf -> new EasyBotConfigPayload(buf.readBoolean())
    );

    @Override
    public Type<EasyBotConfigPayload> type() { return ID; }

    public static void handle(EasyBotConfigPayload payload) {
        ChatMessageStore.setEasyBotCompat(payload.easyBotCompat());
    }
}
