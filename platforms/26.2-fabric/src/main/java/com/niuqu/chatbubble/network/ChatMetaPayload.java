package com.niuqu.chatbubble.network;

import Type;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ChatMetaPayload(UUID senderUUID, String senderName, String messageHash,
                               String quoteSender, String quoteContent, List<String> mentionTargets)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ChatMetaPayload> ID =
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("e33chat", "chat_meta"));

    public static final StreamCodec<FriendlyByteBuf, ChatMetaPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeUtf(value.senderUUID.toString());
            buf.writeUtf(value.senderName);
            buf.writeUtf(value.messageHash);
            buf.writeUtf(value.quoteSender);
            buf.writeUtf(value.quoteContent);
            buf.writeCollection(value.mentionTargets, FriendlyByteBuf::writeUtf);
        },
        buf -> new ChatMetaPayload(
            UUID.fromString(buf.readUtf()),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            buf.readUtf(),
            readMentions(buf)
        )
    );

    /** Mention lists are at most the player count; the count arrives off the
     *  wire, so it is clamped before allocating. The library list codec would
     *  still preallocate up to 65536 entries; parity with Forge's packet is 200.
     *  Wire format is unchanged (varint count + strings). */
    private static final int MAX_MENTIONS = 200;

    private static List<String> readMentions(FriendlyByteBuf buf) {
        int count = Math.min(Math.max(buf.readVarInt(), 0), MAX_MENTIONS);
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) out.add(buf.readUtf());
        return out;
    }

    @Override
    public Type<ChatMetaPayload> type() { return ID; }
}
