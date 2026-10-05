package com.niuqu.chatbubble.compat;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;

import net.fabricmc.loader.api.FabricLoader;

/**
 * chatprefix（服务端聊天前缀 mod）共存处理。
 *
 * <p>为什么需要这个类：两个 mod 都会在玩家进服时补发历史消息，但语义完全不同 ——
 * chatprefix 是"你不在的那段时间别人说了什么"，e33chat 是"最近 {@code HISTORY_MAX} 条
 * 原样回放给所有人"。两个都开着，进服就会看到两段历史，内容还大量重叠。
 *
 * <p>这里只保留 chatprefix 的补发：它知道每个玩家的 {@code lastSeen}，
 * 能算准"离线期间"和"新人的最近 15 天"，e33chat 的固定 50 条做不到这一点。
 *
 * <p>注意只拦截**自动**补发。{@code /e33chat history} 是玩家自己敲的，照常工作 ——
 * 把显式命令也吞掉只会让人以为坏了。
 */
public final class ChatPrefixCompat {

    private static final String MOD_ID = "chatprefix";
    /** chatprefix 自己的配置文件（相对 config 目录）。 */
    private static final String CONFIG_RELATIVE = "chatprefix/config.json";

    private static Boolean loaded;
    private static boolean noticedDefer;
    private static boolean noticedKeep;

    private ChatPrefixCompat() {}

    /** chatprefix 是否在场。结果缓存：mod 列表在运行期不会变。 */
    public static boolean isLoaded() {
        if (loaded == null) {
            Boolean detected;
            try {
                detected = FabricLoader.getInstance().isModLoaded(MOD_ID);
            } catch (Throwable t) {
                // 极端情况下 loader 不可用（比如被别的工具加载进来），当成"没有"，
                // 宁可多发一次历史，也不要因为探测失败把进服流程搞崩
                detected = Boolean.FALSE;
            }
            loaded = detected;
        }
        return loaded;
    }

    /**
     * chatprefix 那边**实际会不会**补发。
     *
     * <p>【为什么不能只看"装没装"】最早这里只判断 mod 存在性，于是有个静默陷阱：
     * 服务器同时装了 E33Chat 和 chatprefix，只要 chatprefix 在场，E33Chat 就让出补发；
     * 可一旦管理员把 chatprefix 的 {@code history.enabled} 关掉，chatprefix 什么都不补、
     * E33Chat 也被禁掉了 —— 玩家进服**两边都没补**，而且症状不指向任何一个 mod
     * （关掉的那一方看起来是"我关了历史"，另一边看起来是"我在让位"）。
     *
     * <p>所以判据必须是"它实际补不补"，而不是"它在不在"。为此读一眼 chatprefix 的
     * {@code config/chatprefix/config.json}：缺文件或缺键都按 chatprefix 自己的默认值
     * （{@code enabled = true}）算。
     *
     * <p>读不出来（IO 异常、格式不认识）时返回 <b>false</b> = E33Chat 照旧自己补。
     * 方向是刻意的：宁可重复一遍历史，也不要两边都沉默。
     *
     * <p>不缓存：只在进服时调用一次，读一个小文件的代价可以忽略，而缓存会在
     * {@code /chatprefix reload} 之后变陈旧。
     */
    public static boolean chatPrefixWillReplay() {
        if (!isLoaded()) {
            return false;
        }
        try {
            Path cfg = FabricLoader.getInstance().getConfigDir().resolve(CONFIG_RELATIVE);
            if (!Files.exists(cfg)) {
                // chatprefix 还没写过配置 = 全用默认值，默认是开着补发的
                return true;
            }
            try (Reader in = Files.newBufferedReader(cfg, StandardCharsets.UTF_8)) {
                JsonObject raw = JsonParser.parseReader(in).getAsJsonObject();
                if (!raw.has("history") || !raw.get("history").isJsonObject()) {
                    return true;
                }
                JsonObject history = raw.getAsJsonObject("history");
                if (!history.has("enabled") || history.get("enabled").isJsonNull()) {
                    return true;
                }
                return history.get("enabled").getAsBoolean();
            }
        } catch (Throwable t) {
            LogUtils.getLogger().warn(
                "[e33chat] 读不到 chatprefix 配置，无法确认它是否会补发历史；"
                    + "本次仍由 e33chat 自己补（宁可重复也不漏）");
            return false;
        }
    }

    /**
     * 自动补发是否已经交给 chatprefix 了。
     *
     * @return true 表示 e33chat 这次不要发
     */
    public static boolean historyReplaySuperseded() {
        if (!chatPrefixWillReplay()) {
            if (isLoaded() && !noticedKeep) {
                noticedKeep = true;
                LogUtils.getLogger().info(
                    "[e33chat] chatprefix 在场但它的历史补发是关的 —— 本次仍由 e33chat 自己补发");
            }
            return false;
        }
        if (!noticedDefer) {
            noticedDefer = true;
            LogUtils.getLogger().info(
                "[e33chat] chatprefix 会补发历史：进服补发已交给它，本次跳过 e33chat 的历史同步");
        }
        return true;
    }
}
