package com.niuqu.chatbubble.network;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record HistoryPayload(List<HistoryPayload.HistoryEntry> entries)
        implements CustomPacketPayload {

    /** Cracked/offline senders already arrive as UUID(0,0); reuse it for a missing one. */
    private static final UUID NULL_UUID = new UUID(0, 0);

    public static final CustomPacketPayload.Type<HistoryPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "chat_history"));

    public record HistoryEntry(
        UUID senderUUID,
        String senderName,
        String content,
        long time,
        boolean isSystem,
        String replyContent,
        String replySender,
        String group
    ) {}

    public static final StreamCodec<FriendlyByteBuf, HistoryPayload> CODEC = StreamCodec.ofMember(
        // Robustness, learned from a field incident: this packet is built from a
        // snapshot of the server's history buffer and encoded while the join-event
        // chain is still running, so one null row used to throw an NPE that cost the
        // joining player their login ("Invalid player data"). Rows that cannot be
        // encoded are skipped; writeCollection's count is the filtered list's size,
        // so the header can never over-count what follows.
        (value, buf) -> {
            List<HistoryEntry> rows = new ArrayList<>();
            if (value.entries != null) {
                for (int i = 0; i < value.entries.size(); i++) {
                    HistoryEntry e = value.entries.get(i);
                    if (e != null) rows.add(e);
                }
            }
            if (value.entries != null && rows.size() < value.entries.size()) {
                // One line of evidence for "the history arrived short": dropping a row
                // is deliberate, but it must not be invisible.
                com.mojang.logging.LogUtils.getLogger().warn("[e33chat] History packet: dropped "
                    + (value.entries.size() - rows.size()) + " null row(s) of "
                    + value.entries.size());
            }
            buf.writeCollection(rows, HistoryPayload::writeEntry);
        },
        buf -> {
            // Entry bound aligned with Forge/Neo (200): the count comes off the
            // wire, and even 1.21.1's readList still allows a 65536-entry
            // allocation per packet. Entries is the payload's only field, so
            // leftover bytes from an oversized count are harmless.
            int count = Math.min(Math.max(buf.readVarInt(), 0), 200);
            List<HistoryEntry> entries = new ArrayList<>(count);
            for (int i = 0; i < count; i++) entries.add(new HistoryEntry(
                UUID.fromString(buf.readUtf()),
                buf.readUtf(),
                buf.readUtf(),
                buf.readLong(),
                buf.readBoolean(),
                nullOrEmpty(buf.readUtf()),
                nullOrEmpty(buf.readUtf()),
                nullOrEmpty(buf.readUtf())
            ));
            return new HistoryPayload(entries);
        }
    );

    private static void writeEntry(FriendlyByteBuf buf, HistoryEntry e) {
        buf.writeUtf((e.senderUUID() != null ? e.senderUUID() : NULL_UUID).toString());
        buf.writeUtf(e.senderName() != null ? e.senderName() : "");
        buf.writeUtf(e.content() != null ? e.content() : "");
        buf.writeLong(e.time());
        buf.writeBoolean(e.isSystem());
        buf.writeUtf(e.replyContent() != null ? e.replyContent() : "");
        buf.writeUtf(e.replySender() != null ? e.replySender() : "");
        buf.writeUtf(e.group() != null ? e.group() : "");
    }

    private static String nullOrEmpty(String s) { return s == null || s.isEmpty() ? null : s; }

    @Override
    public Type<HistoryPayload> type() { return ID; }
}
