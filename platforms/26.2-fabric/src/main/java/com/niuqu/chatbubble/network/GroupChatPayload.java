package com.niuqu.chatbubble.network;

import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C group chat message, sent only to members running the mod (vanilla
 * members receive a plain formatted line instead). Carries the full content so
 * the client builds the bubble locally — no text-tag parsing, no echo.
 */
public record GroupChatPayload(UUID senderUUID, String senderName, String groupName,
                               String content, String quoteSender, String quoteContent)
        implements CustomPacketPayload {

    // Server caps content; the codec below caps reads via readString(max)
    private static final int MAX_TEXT = 2048;

    public static final CustomPacketPayload.Type<GroupChatPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "group_chat"));

    public static final StreamCodec<FriendlyByteBuf, GroupChatPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeUUID(value.senderUUID);
            buf.writeUtf(value.senderName, 256);
            buf.writeUtf(value.groupName, 64);
            buf.writeUtf(value.content, MAX_TEXT);
            buf.writeUtf(value.quoteSender != null ? value.quoteSender : "", 256);
            buf.writeUtf(value.quoteContent != null ? value.quoteContent : "", MAX_TEXT);
        },
        buf -> new GroupChatPayload(
            buf.readUUID(),
            buf.readUtf(256),
            buf.readUtf(64),
            buf.readUtf(MAX_TEXT),
            blankToNull(buf.readUtf(256)),
            blankToNull(buf.readUtf(MAX_TEXT))
        )
    );

    private static String blankToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** Client-side hook, invoked from ChatBubbleClientSetup's receiver. */
    public static void handleClient(GroupChatPayload payload) {
        com.niuqu.chatbubble.store.ChatMessageStore.addGroupMessage(
            net.minecraft.network.chat.Component.literal(payload.content()),
            payload.senderUUID(),
            net.minecraft.network.chat.Component.literal(payload.senderName()),
            payload.groupName(), payload.quoteSender(), payload.quoteContent());
    }
}
