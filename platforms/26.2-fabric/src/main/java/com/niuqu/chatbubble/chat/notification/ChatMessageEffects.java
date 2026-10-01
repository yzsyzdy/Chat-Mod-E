package com.niuqu.chatbubble.chat.notification;

import com.niuqu.chatbubble.ChatBubbleClientSetup;
import com.niuqu.chatbubble.store.ChatMessageStore;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * 真实副作用实现（B4 上移）：横幅转发给 MentionNotificationController，
 * 公共/引用提示音直接播放。由 ChatBubbleClientSetup.onInitializeClient()
 * 注册为 ChatMessageStore 的观察者——store 不再直接依赖 Minecraft 单例。
 */
public class ChatMessageEffects implements ChatMessageStore.MessageEffectObserver {
    @Override
    public void onMentionOrQuote(Component content, ChatMessageStore.SenderMeta meta,
                                 int index, String replySender) {
        MentionNotificationController.INSTANCE.onMessageCaptured(content, meta, index, replySender);
    }

    @Override
    public void onWhisperReceived(UUID senderUUID, Component senderName, Component content, int index) {
        MentionNotificationController.INSTANCE.onWhisperReceived(senderUUID, senderName, content, index);
    }

    @Override
    public void onSystemMessage(Component content, int index) {
        MentionNotificationController.INSTANCE.onSystemMessage(content, index);
    }

    @Override
    public void onPublicChatSound() {
        playChime();
    }

    @Override
    public void onQuoteSound() {
        playChime();
    }

    private static void playChime() {
        NotificationSoundGate.tryPlay(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null)
                // 26.2：音效常量去掉了 BLOCK_ 前缀（BLOCK_NOTE_BLOCK_CHIME -> NOTE_BLOCK_CHIME），
                // 类型仍是 Holder.Reference<SoundEvent>，所以取值方式 value() 不变。
                player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6F * ChatBubbleClientSetup.config().soundVolume() / 100f, 1.0F);
        });
    }
}
