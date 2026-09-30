package com.niuqu.chatbubble.network;

import com.niuqu.chatbubble.config.ServerConfig;
import com.niuqu.chatbubble.config.ServerConfigManager;
import com.niuqu.chatbubble.chat.TemplateMatcher;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client -> server: save the server-config GUI edits. The server re-validates
 * every template, persists to the JSON file, and rebroadcasts to all players.
 */
public record ServerConfigSavePayload(boolean useTpa, boolean historyEnabled, boolean templateDebug,
                                      boolean mediaEnabled, boolean mediaAutoClean, boolean easyBotCompat,
                                      boolean groupsEnabled,
                                      List<String> chatTemplates, List<String> whisperTemplates)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ServerConfigSavePayload> ID =
        new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("e33chat", "server_config_save"));

    public static final StreamCodec<FriendlyByteBuf, ServerConfigSavePayload> CODEC = StreamCodec.ofMember(
        (value, buf) -> ServerConfigDto.encode(new ServerConfigDto(
            value.useTpa, value.historyEnabled, value.templateDebug, value.mediaEnabled,
            value.mediaAutoClean, value.easyBotCompat, value.groupsEnabled, value.chatTemplates, value.whisperTemplates), buf),
        buf -> {
            ServerConfigDto d = ServerConfigDto.decode(buf);
            return new ServerConfigSavePayload(d.useTpa(), d.historyEnabled(), d.templateDebug(),
                d.mediaEnabled(), d.mediaAutoClean(), d.easyBotCompat(), d.groupsEnabled(),
                d.chatTemplates(), d.whisperTemplates());
        }
    );

    @Override
    public Type<ServerConfigSavePayload> type() { return ID; }

    /** Server-side handler: validate, persist, rebroadcast (called from ChatBubbleMod). */
    public static void handleServer(ServerConfigSavePayload payload, ServerPlayer player,
                                    java.util.function.Consumer<ServerConfig> applyAndSave) {
        Component error = validateTemplates(true, payload.chatTemplates());
        if (error == null) error = validateTemplates(false, payload.whisperTemplates());
        if (error != null) {
            player.displayClientMessage(Component.translatable("e33chat.server.save_failed", error)
                .withStyle(ChatFormatting.RED), false);
            return;
        }
        ServerConfig cfg = new ServerConfig();
        cfg.use_tpa = payload.useTpa();
        cfg.history_enabled = payload.historyEnabled();
        cfg.template_debug = payload.templateDebug();
        cfg.media_enabled = payload.mediaEnabled();
        cfg.media_auto_clean = payload.mediaAutoClean();
        cfg.easy_bot_compat = payload.easyBotCompat();
        cfg.groups_enabled = payload.groupsEnabled();
        cfg.chat_templates = new ArrayList<>(payload.chatTemplates());
        cfg.whisper_templates = new ArrayList<>(payload.whisperTemplates());
        applyAndSave.accept(cfg);
        player.displayClientMessage(Component.translatable("e33chat.server.saved"), false);
    }

    private static Component validateTemplates(boolean chat, List<String> templates) {
        for (int i = 0; i < templates.size(); i++) {
            TemplateMatcher.CompileResult result = TemplateMatcher.compile(templates.get(i));
            if (result.template() == null) {
                return Component.translatable("e33chat.server.template_invalid",
                    Component.translatable(chat ? "e33chat.server.kind_chat" : "e33chat.server.kind_whisper"),
                    i + 1, result.error());
            }
        }
        return null;
    }
}
