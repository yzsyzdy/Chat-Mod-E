package com.niuqu.chatbubble.network;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Client -> server: one chunk of a media upload (2.3.13 server-side media
 * hosting). The upload is split into DiskMediaStore.CHUNK_BYTES chunks so a
 * large image stays under the protocol packet size limit.
 */
public record MediaUploadPayload(long uploadId, int index, int totalChunks,
                                 int totalBytes, String contentType, byte[] chunk)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MediaUploadPayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "media_upload"));

    public static final StreamCodec<FriendlyByteBuf, MediaUploadPayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> {
            buf.writeLong(value.uploadId);
            buf.writeInt(value.index);
            buf.writeInt(value.totalChunks);
            buf.writeInt(value.totalBytes);
            buf.writeUtf(value.contentType != null ? value.contentType : "");
            buf.writeByteArray(value.chunk);
        },
        buf -> new MediaUploadPayload(
            buf.readLong(),
            buf.readInt(),
            buf.readInt(),
            buf.readInt(),
            // Read bounds aligned with the NeoForge payload: a hostile client
            // must not be able to push oversized strings or chunks past the
            // decoder (out-of-range throws, so the packet is dropped whole).
            buf.readUtf(128),
            buf.readByteArray(com.niuqu.chatbubble.server.DiskMediaStore.CHUNK_BYTES)
        )
    );

    @Override
    public Type<MediaUploadPayload> type() { return ID; }
}
