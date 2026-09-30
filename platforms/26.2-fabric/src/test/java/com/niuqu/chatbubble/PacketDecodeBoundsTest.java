package com.niuqu.chatbubble;

import com.niuqu.chatbubble.network.ConfigSyncV2Payload;
import com.niuqu.chatbubble.network.MediaUploadPayload;
import com.niuqu.chatbubble.network.ServerConfigDto;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Hostile-input contract for decoders that take a length prefix off the wire
 * (see the Forge/Neo twin). The caps exist so one crafted packet cannot make
 * the peer preallocate an arbitrarily large collection.
 */
class PacketDecodeBoundsTest {

    private static FriendlyByteBuf buf() { return new FriendlyByteBuf(Unpooled.buffer()); }

    @Test
    void configSyncV2TemplateCountIsCapped() {
        FriendlyByteBuf b = buf();
        b.writeBoolean(true);
        b.writeInt(Integer.MAX_VALUE);   // hostile chat-template count
        b.writeInt(0);
        b.writeBoolean(false);
        assertDoesNotThrow(() -> {
            try { ConfigSyncV2Payload.CODEC.decode(b); }
            catch (RuntimeException expectedEof) { /* fine */ }
        });
    }

    @Test
    void serverConfigDtoTemplateCountIsCapped() {
        FriendlyByteBuf b = buf();
        for (int i = 0; i < 7; i++) b.writeBoolean(false);
        b.writeInt(Integer.MAX_VALUE);   // hostile chat-template count
        b.writeInt(0);
        assertDoesNotThrow(() -> {
            try { ServerConfigDto.decode(b); }
            catch (RuntimeException expectedEof) { /* fine */ }
        });
    }

    @Test
    void mediaUploadOversizedStringIsRejectedNotClamped() {
        FriendlyByteBuf b = buf();
        b.writeLong(1L); b.writeInt(0); b.writeInt(1); b.writeInt(1);
        b.writeInt(Integer.MAX_VALUE);   // hostile content-type length
        assertThrows(RuntimeException.class, () -> MediaUploadPayload.CODEC.decode(b),
            "an out-of-range length must be rejected, not silently clamped");
    }

    @Test
    void mediaUploadOversizedChunkIsRejected() {
        FriendlyByteBuf b = buf();
        b.writeLong(1L); b.writeInt(0); b.writeInt(1); b.writeInt(1);
        b.writeUtf("image/png");
        b.writeInt(Integer.MAX_VALUE);   // hostile chunk length
        assertThrows(RuntimeException.class, () -> MediaUploadPayload.CODEC.decode(b));
    }

    @Test
    void chatMetaMentionCountIsCapped() {
        FriendlyByteBuf b = buf();
        b.writeUtf(new java.util.UUID(0L, 0L).toString());
        b.writeUtf("Alex"); b.writeUtf("h"); b.writeUtf("Steve"); b.writeUtf("quoted");
        b.writeVarInt(Integer.MAX_VALUE);   // hostile mention count
        for (int i = 0; i < 200; i++) b.writeUtf("t" + i);
        com.niuqu.chatbubble.network.ChatMetaPayload p =
            com.niuqu.chatbubble.network.ChatMetaPayload.CODEC.decode(b);
        assertEquals(200, p.mentionTargets().size(),
            "count must clamp to the cap instead of trusting a hostile length");
    }
}
