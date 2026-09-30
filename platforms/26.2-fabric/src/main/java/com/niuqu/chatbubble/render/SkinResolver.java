package com.niuqu.chatbubble.render;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

/**
 * Player-head skin resolution with a merged UUID + name cache.
 *
 * Extracted from ChatBubbleScreen / ChatSidebar during the 2.3.14 restructure:
 * the two components previously kept separate LRU caches for the same data.
 * The resolution fallback chain (online fresh read -> uuid cache -> name cache ->
 * SkinProvider -> default) is unchanged; only the cache store is shared.
 */
public final class SkinResolver {
    private SkinResolver() {}

    private static final int SKIN_CACHE_CAP = 256;
    private static final UUID NIL_UUID = new UUID(0, 0);

    private static final Map<UUID, ResourceLocation> skinCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, ResourceLocation> eldest) {
            return size() > SKIN_CACHE_CAP;
        }
    };

    // Name-keyed skin cache: an offline player seen in chat history keeps the
    // real head when the UUID lookup fails (cracked servers, uuid dropped in
    // old history files). Key is the §-stripped lowercase name.
    private static final Map<String, ResourceLocation> skinNameCache = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ResourceLocation> eldest) {
            return size() > SKIN_CACHE_CAP;
        }
    };

    private static String skinNameKey(String name) {
        if (name == null) return null;
        String key = name.replaceAll("§.", "").trim().toLowerCase(java.util.Locale.ROOT);
        return key.isEmpty() ? null : key;
    }

    private static void rememberSkin(UUID uuid, String name, ResourceLocation tex) {
        if (tex == null) return;
        if (uuid != null && !uuid.equals(NIL_UUID)) skinCache.put(uuid, tex);
        String key = skinNameKey(name);
        if (key != null) skinNameCache.put(key, tex);
    }

    public static ResourceLocation getSkin(UUID uuid, String name) {
        Minecraft client = Minecraft.getInstance();
        // Online players: read PlayerListEntry fresh every frame — caching the first
        // result (default Steve/Alex while the async download is in progress) would
        // freeze the head forever even after the real skin loaded. CSL intercepts the
        // underlying lookup.
        if (client.getConnection() != null && uuid != null && !uuid.equals(NIL_UUID)) {
            PlayerInfo info = client.getConnection().getPlayerInfo(uuid);
            if (info != null) {
                ResourceLocation tex = info.getSkin().texture();
                rememberSkin(uuid, name, tex);
                return tex;
            }
        }
        if (uuid != null && !uuid.equals(NIL_UUID)) {
            ResourceLocation cached = skinCache.get(uuid);
            if (cached != null) return cached;
        }
        String nameKey = skinNameKey(name);
        if (nameKey != null) {
            ResourceLocation cachedByName = skinNameCache.get(nameKey);
            if (cachedByName != null) return cachedByName;
        }
        ResourceLocation resolved = resolveSkin(uuid, name);
        rememberSkin(uuid, name, resolved);
        return resolved;
    }

    private static ResourceLocation resolveSkin(UUID uuid, String name) {
        Minecraft client = Minecraft.getInstance();
        // Route through PlayerSkinProvider with a name-bearing GameProfile so CSL
        // can match offline players to imported skins. getSkinTextures(GameProfile)
        // is the Yarn equivalent of Mojang's SkinManager.getInsecureSkin().
        if (name != null && !name.isEmpty()) {
            try {
                GameProfile profile = new GameProfile(
                    uuid != null && !uuid.equals(NIL_UUID) ? uuid : NIL_UUID, name);
                return client.getSkinManager().getInsecureSkin(profile).texture();
            } catch (Exception ignored) {}
        }
        return DefaultPlayerSkin.getDefaultTexture();
    }
}
