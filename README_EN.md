[简体中文](README.md) | [English](README_EN.md)

<h1 align="center">E33Chat</h1>

<p align="center">
  <em>Rebuilds the vanilla chat HUD in chat-app style</em>
</p>

<p align="center">
  <img alt="MC" src="https://img.shields.io/badge/MC-26.2-green">
  <img alt="Loader" src="https://img.shields.io/badge/Loader-Fabric-orange">
  <img alt="Side" src="https://img.shields.io/badge/Side-Client%20required,%20server%20optional-blue">
  <img alt="Java" src="https://img.shields.io/badge/Java-25%2B-yellow">
  <img alt="Version" src="https://img.shields.io/badge/Version-2.4.18-informational">
  <img alt="License" src="https://img.shields.io/badge/License-MIT-brightgreen">
</p>

E33Chat is a chat-enhancement mod that rebuilds the vanilla chat HUD in a chat-app style: bubbles with heads, @ mentions, a whisper sidebar, search, emoji & quick phrases, image messages, quote reply, notification banners, local chat history, and a fully reworked settings screen.

> [!IMPORTANT]
> **This repository targets Minecraft 26.2 on Fabric only.** The upstream three-platform layout (1.20.1 Forge + 1.21.1 NeoForge + 1.21.1 Fabric) is retired; only `platforms/26.2-fabric` remains, and the old platform directories and branches are no longer maintained.
>
> Also note: **Minecraft has been unobfuscated since 26.1**, so there are no Yarn mappings here and no remap step — that is intentional, see [Building from source](#building-from-source).

---

## Contents

- [Installation](#installation)
- [Quick start](#quick-start)
- [Features](#features)
- [Usage](#usage)
- [Server-side bonus](#server-side-bonus)
- [Compatibility](#compatibility)
- [Known limitations](#known-limitations)
- [Privacy & data](#privacy--data)
- [FAQ](#faq)
- [Troubleshooting](#troubleshooting)
- [Building from source](#building-from-source)
- [Changelog](#changelog)
- [Reporting issues](#reporting-issues)
- [License](#license)

---

## Installation

| Dependency | Type | Notes |
|---|---|---|
| Minecraft | Required | **26.2** |
| Fabric Loader | Required | 0.19.5+ |
| Fabric API | Required | 0.161.0+26.2 (`fabric-api`, any compatible version) |
| Java | Required | **25+** (required by 26.2; `javaVersion.majorVersion=25`) |
| ModMenu | Optional | 20.0.0+ — needed to reach the settings from the mod list |
| CustomSkinLoader | Optional | Shows offline players' heads |

1. Download `e33chat-Fabric-26.2-2.4.18.jar`
2. Drop it into `.minecraft/mods/`
3. Launch the game (client-side is required; the server side is optional and unlocks extra features — see [Server-side bonus](#server-side-bonus))

---

## Quick start

1. Press **T / Enter** to open chat and you get the E33Chat panel. **There is no custom keybind** — it uses the vanilla chat key; the `[T]` on the HUD icon simply shows which key you have bound
2. Bottom-left **gear** → menu → Settings opens the config screen
3. The "Chat" tab controls panel width (400–1600 physical pixels, unaffected by GUI scale; "fullscreen panel" fills the screen), bubble colours, corner radius, message gap and avatar size
4. The "Notify" tab configures the @ ping, banners, whisper sounds and master volume; banner offset (`banner_offset_x/y`) lets you dodge other HUD elements
5. Send an image: **upload button** on the left / **Ctrl+V** / **drag an image into the window** — it is sent as soon as the upload finishes

---

## Features

- 💬 **Chat bubbles** — with head and name; colours, text colour, corner radius and theme are configurable. Heads are top-aligned, consecutive messages from one person show the head only on the first (QQ style); gap is 4px within a group and 12px between groups
- 🖼️ **Image messages** — renders `[[CICode]]` / `[[ChatUpgrade]]` protocol images natively inside the bubble (interoperable with ChatImage); click to open the original. Rate-limited against spam, with a receive toggle
- ☁️ **Server-side media hosting** — when the server also runs E33Chat, images are stored on the server (permanently); otherwise it falls back to a third-party host
- 😀 **Custom emotes** — drop images into `config/e33chat/emotes/` (up to 32); Ctrl+V adds the clipboard image; click to send
- @ **Mention completion** — type `@` for a player list; left-click a head to mention. Ping + banner when you are mentioned or quoted
- 👥 **Whisper sidebar** — online player list, unread dots, public/whisper split, NPC hide list
- 🔍 **Search, emoji & quick phrases** — live search (CJK aware), emoji/kaomoji panel, one-click quick phrases
- 📋 **Copy & quote reply** — right-click a message to copy/quote; right-click a head to whisper/teleport/block
- 🚫 **Player blocking** — messages disappear entirely (vanilla chat, bubbles, banners, sounds), effective immediately, not restored on rejoin
- 🔔 **Notification banners** — covers @ / quote / whisper / system messages, with a master volume slider, per-type toggles and position offset
- 🎬 **Animation styles** — panel / banner / popup / message each configurable (SLIDE / FADE / ZOOM / NONE); popups animate open and closed
- 🗨️ **Vanilla chat preserved** — still renders normally (shifted up to avoid the HUD icon), so ChatHeads / ChatAnimation etc. keep working
- 💾 **Chat history** — stored per world/server, preserving colours and click events (off by default); autosaves every 30 seconds
- 🛠️ **Config screen** — 5 tabs with collapsible sub-categories, live preview, snapshot save/discard; every colour, number and toggle is adjustable in the GUI
- ✅ Anti-spam merge counter · 📝 keep input on close · 🌈 local `&` colour codes · 🧩 server-declared message templates

---

## Usage

### Chat display

- Your own bubbles align right, others left; colours are configurable per side; corner radius 0–10 (default 4)
- Whispers show as `<name>[whisper] content`, quote replies as `<name>[quote] content` (yellow tag); server prefix decoration and team colours are preserved
- Message gap: consecutive messages from the same sender within 5 minutes = `message_gap` × 2/3 (default 4px); sender change / timeout / system message / time separator = ×2 (default 12px)

### Images & media

- **Sending**: upload button / Ctrl+V / drag & drop; automatically scaled to ≤2048px and re-encoded; uploads are queued serially (up to 8), one Enter is enough, and the input is restored on failure
- **Hosting**: defaults to uguu.se (expires in ~3 hours); with server-side hosting enabled it is stored on the server instead (`e33chat://media/<id>`, permanent); custom hosts use the four `upload_url` keys (multipart POST, response read as raw text or `json:field.path`)
- **Receiving**: image codes render natively, old history images are re-loaded for compatibility; abuse protection = sliding-window rate limit + 64-entry LRU texture cache + downscale before decode; with "receive images" off it shows plain `[image]` and downloads nothing

### Sidebar & notifications

- Sidebar: click a name to whisper, pulsing unread dots, search filter, public/whisper split, wildcard hide list (e.g. `*[NPC]*`)
- Banners: four kinds (@ / quote / whisper / system), system banners on by default; a "jump to mention" button; ±1000px offset to avoid overlapping other HUD elements
- Optional "mentions must carry the @ prefix"; self-notification toggles (off by default, for debugging); a master volume slider

### Animation & appearance

- Four independent animation styles SLIDE / FADE / ZOOM / NONE: panel (SLIDE by default), banner (SLIDE), popup (FADE), message (FADE); `animation=false` disables all of them
- Popups fade in over 200ms and out over 150ms (all paths: ESC / icon toggle / click outside); banners enter over 250ms and leave over 150ms
- Popups (settings / emoji / quick phrases / search / @ popup / context menu) use rounded corners, a shadow and a 1px outline; quote blocks use radius 8
- Panel background blur `blurEnabled` is off by default (and is a no-op on 26.2 — see [Known limitations](#known-limitations))

### Settings & textures

- Dark / light themes; 5 config tabs (Chat / HUD / Notify / Sidebar / Advanced), snapshot save/discard, ESC asks before discarding
- **Resource-pack overrides**: UI elements and icons are texture-rendered from `assets/e33chat/textures/gui/{dark|light}/<element>.png`; F3+T applies changes immediately
- ⚠️ **Popup backgrounds stopped using textures in 2.3.16** (driven by semantic theme colours); chat bubbles, quote blocks and @ banners were always drawn procedurally — neither can be overridden by a resource pack

---

## Server-side bonus

The server side is optional. Installing it additionally enables:

- Quote-reply sync and cross-client @ mention sync (including CJK names)
- New players receive recent chat history on join (`history_enabled`, off by default)
- Head teleport switches to `/tpa` (`use_tpa`, off by default)
- **Server-side image hosting** (`media_enabled`, on by default): images stored on the server permanently (8MB/file, 512MB total quota, random UUIDs to prevent enumeration, per-player rate limit)
- **Message format templates**: the server declares the chat format and syncs it to everyone — messages whose format was changed by plugins / NCR still parse correctly (`/e33chat gui` to configure; "generate from message" or a one-click preset; placeholders `{display_name}` `{prefix}` `{external}` `{content}` `{sender}` `{target}` `{sep}`)
- **EasyBot group-message compatibility** (`easybot_compat`, on by default): QQ group messages relayed into the game by EasyBot are parsed into player bubbles, and EasyBot/ChatImage CICode images render inside the bubble. Common formats are auto-detected (since 2.4.8 the group prefix and QQ number are both optional); if you changed EasyBot's sync template, override it with an `{external}` chat template (the server config ships presets — see [EasyBot templates](#easybot-templates))

**Server config**: `<world dir>/serverconfig/e33chat-server.json` (created on the first player join)

| Command | Permission | Description |
|---|---|---|
| `/e33chat gui` | OP | Graphical config for the server-side options |
| `/e33chat template list` | OP | List current templates |
| `/e33chat template set <chat\|whisper> <template>` | OP | Set a template |
| `/e33chat template remove <chat\|whisper>` | OP | Remove one |
| `/e33chat template clear <chat\|whisper>` | OP | Clear |
| `/e33chat template test <chat\|whisper>` | OP | Try parsing a message with the template |
| `/e33chat group list \| create \| join \| leave \| delete \| msg` | — | Chat groups (when enabled server-side) |

### EasyBot templates

- E33Chat recognises the common EasyBot formats out of the box: `[group] <nick(QQ)> content`, `[group] <nick> content`, `<nick> content`, `<nick (group card)> content` (since 2.4.8 neither the group prefix nor the QQ number is required), so `easybot_compat` works as-is
- If you changed "sync template (to server)" inside EasyBot itself, go to `/e33chat gui` → chat templates and add an `{external}` template to override it
- Common examples:

| EasyBot sync template | E33Chat chat template |
|---|---|
| `[group] <nick(QQ)> content` | `[{prefix}] <{external}> {content}` |
| `[group] nick: content` | `[{prefix}] {external}{sep}{content}` |
| `nick >> content` | `{external}{sep}{content}` |
| `<nick> content` | `<{external}> {content}` |

- Or add it directly: `/e33chat template set chat "[{prefix}] <{external}> {content}"`

---

## Compatibility

| Mod / plugin | Status |
|---|---|
| No Chat Reports and similar report-disabling plugins | Compatible automatically since 2.1.0, no config needed |
| CustomSkinLoader | Shows offline players' heads |
| ChatImage / ChatUpgrade (image protocols) | Native interoperability |
| EasyBot (QQ ↔ Minecraft relay) | `easybot_compat` on by default; group messages become player bubbles and CICode images render inside the bubble |
| IMBlocker | Adapted automatically (command input switches to English) |
| ModernUI | Clickable-text underline and click-area boundary compatibility |
| Quark and similar item sharing | Item icons in system messages render correctly |
| ChatHeads, ChatAnimation | Work by default |
| **MiniHUD / malilib** | The chat panel draws **above** malilib overlays (MiniHUD info lines etc.) — malilib appends them at the TAIL of `Gui.extractRenderState`, later than every screen, so E33Chat deliberately defers its own screen draw until after that. Details in [docs/port-26.2-notes.md](docs/port-26.2-notes.md) |
| Nickname plugins | Partial support, see [FAQ](#faq) |
| Chat-format plugins (EssentialsChat / CMI / DeluxeChat …) | Adaptable through server-side templates (common presets included) |

---

## Known limitations

1. **Minecraft 26.2 + Fabric only, Java 25 required**; the 1.20.1 Forge / 1.21.1 NeoForge / Fabric platforms are retired
2. If a nickname has no relation to the real name and the plugin neither attaches a "click to whisper" event nor syncs the tab name, the message shows as grey system text (templates cannot fix that)
3. Formats where the name and the content are separated by nothing but spaces cannot be recognised; NCR encrypted chat shows ciphertext
4. The default image host expires files after ~3 hours (server hosting is permanent, but the 512MB quota needs watching)
5. **Two rendering downgrades on 26.2** (details in [docs/port-26.2-notes.md](docs/port-26.2-notes.md), section 3):
   - **Rounded corners**: the old implementation used a custom SDF shader; it is now approximated by "a main rectangle plus per-row inset strips". Visually close at radius 6–8px, but not an anti-aliased SDF
   - **Panel background blur** (`blurEnabled`): 26.2 replaced the FBO APIs this relied on, so it is currently a **no-op** — no blur, but no crash either. 26.2 does have a native full-screen blur, but it can only be called once per frame and means "blur everything before this layer", so it cannot do a local blur
   - ~~Texture alpha / tint broken~~ — this one is **fixed**: the colour parameter was there all along, at the end of the `blit` parameter list

---

## Privacy & data

> [!WARNING]
> Chat history is stored **in plain text** on your machine. Do not enable it on a public or untrusted computer.

- **Chat history**: local only, off by default, never uploaded. Path: `<game dir>/e33chat/history/<world name>_<short hash>.json` (one JSON object per line, preserving colours and click events). Commands that carry credentials (`/login`, `/register`, …) are skipped
- **Images**: images you send are uploaded to a third-party host (uguu.se by default, expiring in ~3 hours) or to the server's storage (when server hosting is on). The client never uploads anything on its own
- **The server-side mod** only relays (@ / quotes / history / media) and collects no client data; history saving, syncing and hosting can all be turned off

---

## FAQ

**Do I need it on the server?** No. Installing it unlocks quote sync, @ sync, history sync, `/tpa` teleport and image hosting.

**How do I open the config?** Bottom-left gear → menu → Settings in the panel, or via ModMenu from the mod list. The client config is `config/e33chat/e33chat-client.json`; everything is adjustable in the GUI.

**Is there a keybind?** No custom keybind. The panel uses the vanilla chat key (T by default) — the `[T]` on the HUD icon tells you which key that is.

**Is chat history saved by default?** No (`chatHistoryEnabled: false`). Enable it in Settings → Chat → Chat history. It autosaves every 30 seconds and on a clean exit; a crash loses at most 30 seconds.

**How do I enable background blur?** You can, but it **has no effect on 26.2** (`BlurRenderer` is a no-op — see [Known limitations](#known-limitations)).

**How do I change panel width / make it fullscreen?** `panelWidth` (default 1000) is in physical screen pixels: set it to 800 and the panel occupies exactly 800 physical pixels at any window size or GUI scale, without shifting or clipping (it clamps to the window width if the window is narrower). Range 400–1600. To fill the screen, enable `panelFullscreen` (off by default), which ignores `panelWidth` and keeps the sidebar clickable.

**Where did my image go?** With server hosting on (the default) it is stored on the server permanently; otherwise it goes to the third-party host (uguu.se, ~3 hours). Both are configurable.

**Why is a message grey?** When the client cannot be sure a player said it, it conservatively falls back to grey (see [Known limitations](#known-limitations)); nickname plugins and unusual broadcast formats are the usual causes.

**Do nickname plugins work?** Partially: if the nickname carries a "click to whisper" event or the tab name is synced, attribution works; otherwise the message is grey.

**The server changed its chat format and messages no longer match?** Use message format templates: as OP run `/e33chat gui`, use "generate from message" with a real chat line or pick a preset, and save — it syncs to everyone. Leaving the template empty restores heuristic detection.

**Clicking links / player names does nothing?** Fixed in 2.4.18: 26.2 split the vanilla `handleTextClick` into two methods, and earlier builds only wired up the one handling URLs, which silently broke every `run_command` click (`/msg`, `/warp`, `/tpa`, clickable menus, relayed QQ links). Update and it works.

**How do I go back to vanilla chat?** Settings → Chat → turn off "Enable E33Chat" (`enabled: false`); removing the mod restores everything.

**Can I include it in a modpack?** Yes, no extra permission needed.

---

## Troubleshooting

1. Make sure it is **Minecraft 26.2 + Fabric + Java 25**, and that you are using a JAR built from this repository (old-platform JARs will not load)
2. Back up and delete `config/e33chat/e33chat-client.json` to rule out config corruption; keep only E33Chat to rule out conflicts
3. Image upload fails: check `upload_url` and your network; the default host uguu.se is unreachable from some networks — use a custom host or enable server hosting
4. Check `.minecraft/logs/latest.log` for `[e33chat]` errors; on a crash, read the topmost report in `crash-reports/` — its `Description` and first stack frames
5. When filing an issue, include the version, mod list, `latest.log`, screenshots and reproduction steps

---

## Building from source

### Requirements

- **JDK 25** (hard requirement of 26.2); point `JAVA_HOME` at it
- Use the Gradle wrapper bundled in the repo; no separate Gradle install needed

### Build

```bash
cd platforms/26.2-fabric
./gradlew build          # Windows: gradlew.bat build
```

The artifact lands in `platforms/26.2-fabric/build/libs/e33chat-Fabric-26.2-2.4.18.jar`.

Run the tests:

```bash
./gradlew test
```

### Repository layout

```
gradle.properties              repo-level identity (mod_version etc.)
gradle/e33chat-layers.gradle   shared-layer assembly script
versions/*.json                layer / target / dependency definitions (the script reads
                               these, so no platform hardcodes a target name)
shared/src/main/java/...       platform-neutral code (chat / compat / image / mixin / render / server)
platforms/26.2-fabric/         the only target: Fabric 26.2
  src/main/java/...            Fabric-side implementation (config / network / store / texture / ui / command …)
  src/main/resources/          fabric.mod.json, mixin configs, textures and language files
  src/test/java/...            JUnit 5
docs/port-26.2-notes.md        API changes, rendering downgrades and gotchas of the 26.2 port
ARCHITECTURE.md                overall architecture
CHANGELOG.md                   change log
```

`platforms/26.2-fabric/build.gradle` does `apply from: '../../gradle/e33chat-layers.gradle'`, which wires `shared/` and the layers mounted for this target into the source set; all of the decisions live in `versions/*.json`.

### Things to know when changing code on 26.2

These were all hit for real during the port, so they are worth a look before you start:

- **No Yarn mappings and no remap.** Minecraft has been unobfuscated since 26.1, so `com.mojang:minecraft` already gives readable official names; dependencies use `implementation` rather than `modImplementation` (Loom 1.18 also dropped the whole `mod*` configuration family)
- **`Gui.extractRenderState(DeltaTracker, boolean renderHud, boolean renderScreen)` handles both the HUD and the current screen in a single call**, internally `Hud` → `overlay` → `Screen`. Cancelling at its HEAD therefore **cancels the screen too**; to skip only the HUD, intercept the single `Hud.extractRenderState` call
- **Do not call `extractBackground` manually**: `Screen.extractRenderStateWithTooltipAndSubtitles` already calls it before `extractRenderState`, and a second call throws `IllegalStateException: Can only blur once per frame`. **Overriding** `extractBackground` in a screen is right; **calling** it from `extractRenderState` is wrong
- **`blit` does have colour overloads**, with the colour last in the parameter list (the 10/11-arg and 12/13-arg families). Before concluding a new API lacks a capability, lay out the whole overload family by arity
- **`Screen.handleTextClick` is gone**, split into two complementary static methods: `defaultHandleClickEvent` (URL / open file / command suggestion / clipboard) and `defaultHandleGameClickEvent` (`run_command` / `show_dialog` / `custom`). Wiring up only the former silently breaks "click to run a command"
- **The pose is 2D** (`Matrix3x2fStack`) and `translate` has no z component — code that used z for layering now depends purely on draw order
- **Mixin targets are strings resolved at runtime**: compiling is not proof that a mixin applies. No mixin here sets `require = 0`, so a wrong target fails loudly instead of silently
- **MixinExtras is bundled with Fabric Loader** (`META-INF/jars/mixinextras-fabric-0.5.5.jar` inside 0.19.5), so `@WrapOperation` / `@Local` are available with no extra dependency
- **Elements registered through `HudElementRegistry` live inside the HUD layer**, earlier than overlays that mods like malilib append at the TAIL of `extractRenderState` — to draw above those you must hook later

### Two machine-local workarounds (this Windows box)

Unrelated to the repository; a machine with direct Maven access does not need them:

1. The JDK's bundled cacerts is missing Let's Encrypt's 2026 root `Root YR`, so Gradle downloads fail the handshake → a copy of the JDK was made with that root imported
2. `repo.maven.apache.org` is hijacked by a local middlebox (it presents a self-signed certificate) → an `init.gradle` swaps the repositories for mirrors

The exact commands are in [docs/port-26.2-notes.md](docs/port-26.2-notes.md), section 4.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full history, and [docs/port-26.2-notes.md](docs/port-26.2-notes.md) for the details of the 26.2 port.

---

## Reporting issues

File them at [Issues](https://github.com/yzsyzdy/Chat-Mod-E/issues) with the version, loader, mod list, `latest.log`, screenshots or video, and reproduction steps.

> This repository is the Minecraft 26.2 port of [NoWordz/Chat-Mod-E](https://github.com/NoWordz/Chat-Mod-E). If the problem is specific to the 26.2 port (for example a rendering glitch that only happens on 26.2), please say so and include the stack trace from the matching report in `crash-reports/`.

---

## License

[MIT License](LICENSE)

Copyright &copy; 2026 E33EPUS
