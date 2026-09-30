package com.niuqu.chatbubble.network;

import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S2C group directory sync: pushed on hello, login and after every group
 * mutation. The client only shows the tab strip once a payload with
 * enabled=true arrived — on vanilla servers / singleplayer the feature stays
 * hidden entirely.
 */
public record GroupListPayload(boolean enabled, List<String> names,
                               List<Integer> memberCounts, List<String> myGroups)
        implements CustomPacketPayload {

    // Decode-side caps: a hostile server must not balloon client memory
    private static final int MAX_GROUPS = 200;

    public static final CustomPacketPayload.Type<GroupListPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "group_list"));

    public static final StreamCodec<FriendlyByteBuf, GroupListPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeBoolean(value.enabled);
            buf.writeCollection(value.names, (b, s) -> b.writeUtf(s, 64));
            buf.writeCollection(value.memberCounts, (b, c) -> b.writeVarInt(c));
            buf.writeCollection(value.myGroups, (b, s) -> b.writeUtf(s, 64));
        },
        buf -> new GroupListPayload(
            buf.readBoolean(),
            cap(buf.readList(b -> b.readUtf(64))),
            cap(buf.readList(FriendlyByteBuf::readVarInt)),
            cap(buf.readList(b -> b.readUtf(64)))
        )
    );

    private static <T> List<T> cap(List<T> list) {
        return list.size() > MAX_GROUPS ? list.subList(0, MAX_GROUPS) : list;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }

    /** Client-side hook, invoked from ChatBubbleClientSetup's receiver. */
    public static void handleClient(GroupListPayload payload) {
        com.niuqu.chatbubble.chat.GroupChannelState.enabled = payload.enabled();
        com.niuqu.chatbubble.chat.GroupChannelState.applyDirectory(
            payload.names(), payload.memberCounts(), payload.myGroups());
    }
}
