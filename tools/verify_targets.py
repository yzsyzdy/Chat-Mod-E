#!/usr/bin/env python3
"""校验 versions/ 里的目标矩阵 —— 本地与 CI 跑的是同一份。

结构照搬 AtomChat 的同名脚本。目标写进 versions/targets.json，本脚本是那句声明的执行者：
  * 矩阵的规则（每个 Minecraft 版本都要凑齐 versions/loaders.json 里列出的加载器）
  * buildable: true 的目标不许留 unverified
  * 工程目录存在就必须在矩阵里，反之亦然（不许有"没有任何地方编"的源码）
  * 各平台不许自己定义身份键（mod_version 等只在仓库根那一份）
  * 矩阵里写的 java / gradle 与工程里实际的 toolchain / wrapper 一致（防止两边各写一份然后漂移）
  * 钉等清单（versions/pinned-equal.json）里的多平台副本必须逐字相同
  * 共享层不许用高于最低目标 Java 级别的语法（模式匹配 switch）
  * 服务端侧的类不许引用客户端类型
  * 依赖方向：shared/ 只许引 shared/；layers/ 只许引 shared/layers，或本加载器家族全部在建目标都有的类

【映射家族那套检查已经撤掉】26.1 起 Minecraft 不再混淆、Fabric 也不再产出 Yarn，全仓只剩
一套官方名，所以不再有 mapping 轴、映射层、孪生副本、mapping-aliases 表。哪天又出现两套
并行命名，再把 check_mapping_twins / mapping 轴 / aliases 加回来。

用法：
  python tools/verify_targets.py                       # 只校验，打印摘要
  python tools/verify_targets.py --matrix-out m.json   # 顺便把 CI 矩阵写成 JSON
"""
from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
REQUIRED_FIELDS = ["minecraft", "loader", "mappings", "java", "gradle", "project", "buildable", "layers"]
IDENTITY_KEYS = [
    "mod_id", "mod_name", "mod_license", "mod_group_id",
    "mod_authors", "mod_description", "mod_version",
]
OWN_PREFIX = "com.niuqu.chatbubble."

errors: list[str] = []
notes: list[str] = []


def fail(msg: str) -> None:
    errors.append(msg)


def load_json(rel: str) -> dict:
    path = ROOT / rel
    if not path.is_file():
        fail(f"{rel} 不存在")
        return {}
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except json.JSONDecodeError as exc:
        fail(f"{rel} 不是合法 JSON：{exc}")
        return {}


def entries_of(raw: dict) -> dict:
    return {k: v for k, v in raw.items() if not k.startswith("_")}


def read_props(path: pathlib.Path) -> dict:
    out = {}
    if not path.is_file():
        return out
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        line = line.strip()
        if not line or line.startswith("#") or "=" not in line:
            continue
        k, v = line.split("=", 1)
        out[k.strip()] = v.strip()
    return out


def check_root_identity() -> None:
    props = read_props(ROOT / "gradle.properties")
    if not props:
        fail("仓库根的 gradle.properties 不存在或读不出键 —— 身份没有来源")
        return
    missing = [k for k in IDENTITY_KEYS if k not in props]
    if missing:
        fail(f"仓库根的 gradle.properties 缺身份键：{', '.join(missing)}")


def check_targets(targets: dict, layers: dict, loaders: list) -> None:
    if not targets:
        fail("versions/targets.json 里一个目标都没有 —— 文件坏了或被改空了")
        return

    seen_combo = set()

    for name, t in targets.items():
        for field in REQUIRED_FIELDS:
            if field not in t:
                fail(f"目标 {name} 缺字段 {field}")
        if any(f not in t for f in REQUIRED_FIELDS):
            continue

        mc, loader = t["minecraft"], t["loader"]
        seen_combo.add((mc, loader))

        for layer in t["layers"]:
            if layer not in layers:
                fail(f"目标 {name} 引用了 layers.json 里没有的层：{layer}")
                continue
            gap = predicate_gap(t, layer, layers[layer])
            if gap:
                fail(f"目标 {name} 挂了层 {layer}，但它满足不了该层的谓词：{gap}")

        project = ROOT / t["project"]
        exists = project.is_dir()

        if t["buildable"] and not exists:
            fail(f"目标 {name} 声明 buildable: true，但工程目录 {t['project']} 不存在")
        if t["buildable"] and t.get("unverified"):
            fail(f"目标 {name} 是 buildable: true，却还留着 unverified：{t['unverified']}")
        if exists:
            for needed in ("build.gradle", "settings.gradle", "gradlew"):
                if not (project / needed).is_file():
                    fail(f"目标 {name} 的工程目录缺 {needed}")
            check_platform_identity(name, project)
            check_platform_toolchain(name, project, t)

    # 映射层的双向约束已经随映射家族一起撤掉（26.1 起只剩一套官方名），
    # 相关的 check_mapping_twins / mapping-aliases 检查不再存在。

    # 矩阵是规则：每个出现过的 Minecraft 版本都要凑齐 loaders.json 里列出的加载器
    if not loaders:
        notes.append("versions/loaders.json 的 loaders 是空的 —— 本轮没有校验加载器覆盖度")
    mcs = sorted({mc for mc, _ in seen_combo})
    for mc in mcs:
        for loader in loaders:
            if (mc, loader) not in seen_combo:
                fail(f"Minecraft {mc} 缺 {loader} 条目 —— 新增一个版本 = 为每个加载器各加一条；"
                     f"实在不做也要写 buildable: false 并在 note 里写清为什么，缺了要在这里看得见")

    # 工程目录存在就必须在矩阵里
    platforms = ROOT / "platforms"
    if platforms.is_dir():
        declared = {t["project"] for t in targets.values() if isinstance(t.get("project"), str)}
        for child in sorted(p for p in platforms.iterdir() if p.is_dir()):
            rel = f"platforms/{child.name}"
            if rel not in declared:
                fail(f"{rel} 存在但不在 targets.json 里 —— 那样它是一份没有任何地方编的源码")


def check_platform_identity(name: str, project: pathlib.Path) -> None:
    props = read_props(project / "gradle.properties")
    dupes = [k for k in IDENTITY_KEYS if k in props]
    if dupes:
        fail(f"目标 {name} 的 gradle.properties 自己定义了身份键：{', '.join(dupes)} —— "
             f"身份只在仓库根那一份里，这里留一份就会各自漂移")


def check_platform_toolchain(name: str, project: pathlib.Path, t: dict) -> None:
    text = (project / "build.gradle").read_text(encoding="utf-8", errors="replace")
    m = re.search(r"JavaLanguageVersion\.of\((\d+)\)", text)
    if m and int(m.group(1)) != int(t["java"]):
        fail(f"目标 {name} 的 targets.json 写 java={t['java']}，"
             f"但 build.gradle 的 toolchain 是 {m.group(1)}")

    wrapper = project / "gradle" / "wrapper" / "gradle-wrapper.properties"
    if wrapper.is_file():
        m = re.search(r"gradle-([0-9][^-]*)", wrapper.read_text(encoding="utf-8", errors="replace"))
        if m and m.group(1) != str(t["gradle"]):
            fail(f"目标 {name} 的 targets.json 写 gradle={t['gradle']}，"
                 f"但 wrapper 是 {m.group(1)}")


def _root_of(line: str) -> str:
    """从「包名  — 说明」这种声明行里取出包名。"""
    return line.split(" — ")[0].strip()


def layer_axes(d: dict) -> list:
    return [a for a in ("since", "loader", "mappings") if a in d]


def layer_dir(layer_name: str, d: dict) -> pathlib.Path:
    """目录由声明的轴推出来 —— 声明与目录不会互相矛盾。"""
    axis_dirs = {"since": "version", "loader": "loader", "mappings": "mapping"}
    return ROOT / "layers" / "+".join(sorted(axis_dirs[a] for a in layer_axes(d))) / layer_name


def version_at_least(have: str, want: str) -> bool:
    def parts(v):
        return [int(p) if str(p).isdigit() else 0 for p in str(v).split(".")]

    a, b = parts(have), parts(want)
    for i in range(max(len(a), len(b))):
        x, y = (a[i] if i < len(a) else 0), (b[i] if i < len(b) else 0)
        if x != y:
            return x > y
    return True


def predicate_gap(t: dict, layer_name: str, d: dict):
    """目标挂了这个层，但它满足不了该层的谓词 —— 返回原因，满足则 None。"""
    for axis in layer_axes(d):
        want = str(d[axis])
        if axis == "since":
            if not version_at_least(t["minecraft"], want):
                return f"Minecraft {t['minecraft']} 够不到 since {want}"
        else:
            have = str(t.get(axis))
            if have != want:
                return f"它的 {axis} 是 {have}，层要求 {axis}={want}"
    return None


def digest(path: pathlib.Path) -> str:
    """行尾无关的比较。

    工作区是 CRLF、仓库里存的是 LF（`.gitattributes` 的 `* text=auto` 管这件事），
    所以直读字节会把"只有行尾不同"判成内容不同 —— 那样"孪生副本逐字相同"这条检查
    会假阴性（该报的不报）。比较前先折叠行尾，让这条检查只看内容。
    """
    return hashlib.sha1(path.read_bytes().replace(b"\r\n", b"\n")).hexdigest()


def check_pinned_equal(pinned: dict) -> None:
    """钉等清单：同一路径在多个平台各有一份，内容必须逐字相同（行尾无关）。

    清单里的每一项都是「本该进 shared/ 或映射层，暂时进不去」的显式状态 ——
    有闸盯着，它们才不会悄悄漂移。哪天消掉一条，就从清单里删掉它。

    条目可以是字符串（全部平台互等），也可以是 {"path": ..., "platforms": [...]} ——
    后者用于两端的孪生副本内容本就不同的文件：只在列出的平台之间要求逐字相同。

    【单目标时清单为空是正常的】钉等的意义是「两份手抄要同步」；只剩一个平台时
    没有任何一对副本可比，所以清单为空是这类仓库的正确状态，不是闸坏掉了。
    """
    items = pinned.get("pinned", [])
    all_platforms = sorted(p.name for p in (ROOT / "platforms").iterdir() if p.is_dir()) \
        if (ROOT / "platforms").is_dir() else []
    if not items:
        if len(all_platforms) <= 1:
            notes.append(f"钉等清单为空，且只有 {len(all_platforms)} 个平台 —— 没有「两份副本」可漂移，符合预期")
        else:
            fail("versions/pinned-equal.json 的 pinned 清单是空的，但有 "
                 f"{len(all_platforms)} 个平台 —— 要么这条制度废了，要么布局变了")
        return
    for item in items:
        if isinstance(item, str):
            rel, scope = item, None
        elif isinstance(item, dict) and isinstance(item.get("path"), str):
            rel, scope = item["path"], item.get("platforms")
            if not isinstance(scope, list) or not scope:
                fail(f"钉等清单 {rel} 的对象条目缺少非空 platforms 列表 —— 限定不了范围就等于没钉")
                continue
        else:
            fail(f"钉等清单有条目既不是字符串也不是 {{path, platforms}} 对象：{item!r}")
            continue
        platforms = [p for p in (scope if scope is not None else all_platforms) if p in all_platforms]
        copies = {p: ROOT / "platforms" / p / rel for p in platforms if (ROOT / "platforms" / p / rel).is_file()}
        if len(copies) < 2:
            continue  # 只剩一份就没有「漂移」可言（可能是清理到一半，等下一波收编）
        digests = {p: digest(f) for p, f in copies.items()}
        distinct = {v for v in digests.values()}
        if len(distinct) > 1:
            where = ", ".join(sorted(digests))
            scope_note = "" if scope is None else "（限定平台：" + ", ".join(scope) + "）"
            fail(f"钉等清单里的 {rel}{scope_note} 在各平台内容不一致（{where}）—— "
                 f"要么把改动同步到每一份，要么把它收编进 shared//映射层然后从清单删掉")


def check_layers(layers: dict) -> None:
    if not layers:
        fail("versions/layers.json 里一层都没有")
        return
    for name, d in layers.items():
        axes = layer_axes(d)
        if not axes:
            fail(f"层 {name} 一个轴都没钉（since / loader / mappings）—— 一个都不写就是 shared/，不是层")
        if not isinstance(d.get("populated"), bool):
            fail(f"层 {name} 没有声明 populated（true/false）—— 「空」要是显式声明的状态")
        directory = layer_dir(name, d)
        if d.get("populated") and not directory.is_dir():
            fail(f"层 {name} 声明 populated: true，但目录 {directory.relative_to(ROOT).as_posix()} 不存在")
        if directory.is_dir() and not d.get("populated"):
            files = [p for p in directory.rglob("*") if p.is_file()]
            if files:
                fail(f"层 {name} 声明 populated: false，但目录里有 {len(files)} 个文件 —— "
                     f"要么翻成 true，要么把内容挪走")


def check_resource_paths(paths: dict) -> None:
    """逐目标比对资源落点：着色器命名空间、语言文件、显式列出的必备文件。

    放错命名空间不会报错，只会在运行期静默降级（圆角没了 / 界面退回英文），
    所以这里按声明逐项相等来卡 —— 多一个少一个都红。
    """
    targets = paths.get("targets", {})
    if not targets:
        fail("versions/resource-paths.json 缺 targets")
        return
    names = paths.get("shader_names", [])
    exts = paths.get("shader_extensions", [])
    langs = paths.get("languages", [])
    also = paths.get("also_required", [])

    for name, t in targets.items():
        res = ROOT / "platforms" / name / "src/main/resources"
        if not res.is_dir():
            continue  # 还没建起来的目标
        expected = set(also)
        for shader in names:
            for ext in exts:
                expected.add(f"{t['shaders_root']}/{shader}{ext}")
        for lang in langs:
            expected.add(f"{t['lang_root']}/{lang}.json")

        # 只比声明的两类根（着色器 / 语言）与显式列出的文件 —— 元数据与 mixin 配置
        # （fabric.mod.json、mixins.e33chat.json）不属于这里，按「所有 .json」过滤
        # 会把它们算成「多出来的资源」。
        def interesting(rel: str) -> bool:
            return ("/shaders/" in rel or "/lang/" in rel or rel in also)

        actual = {p.relative_to(res).as_posix() for p in res.rglob("*")
                  if p.is_file() and interesting(p.relative_to(res).as_posix())}
        for missing in sorted(expected - actual):
            fail(f"目标 {name} 缺资源 {missing} —— 放错命名空间是静默失效，不报错")
        for extra in sorted(actual - expected):
            fail(f"目标 {name} 多出未声明的资源 {extra} —— 写进 versions/resource-paths.json，"
                 f"或删掉它（多出来的资源不会被加载，只会让人以为生效了）")


def _strip_comments(text):
    """逐行剥掉注释（块注释 + 行注释）。

    不用正则：这份文件要跨三端与 CI 跑到，少一层转义就少一类「本地能跑、CI 不能跑」的麻烦。
    近似：字符串字面量里的 // 也会被剥掉，但共享层这些类里没有那种写法。
    """
    NL = chr(10)  # 不写字面量反斜杠：这份文件要跨三端与 CI，少一层转义少一类环境差异
    out = []
    in_block = False
    for line in text.split(NL):
        if in_block:
            idx = line.find('*/')
            if idx < 0:
                continue
            line = line[idx + 2:]
            in_block = False
        start = line.find('/*')
        while start >= 0:
            idx = line.find('*/', start + 2)
            if idx < 0:
                line = line[:start]
                in_block = True
                break
            line = line[:start] + line[idx + 2:]
            start = line.find('/*', start)
        cut = line.find('//')
        if cut >= 0:
            line = line[:cut]
        out.append(line)
    return NL.join(out)


def check_server_side(cfg: dict) -> None:
    """服务端侧的类不许引用客户端类型（专用服务端上会 NoClassDefFoundError）。

    判据是源码级标记扫描（先剥注释）。patterns 是匹配式而不是清单 —— 新加的服务端类自动
    被覆盖，不必记得来登记；每个匹配式至少命中一个文件，命中为零说明匹配式坏了或布局变了，
    那也要红（否则这类检查会悄悄变成什么都不查）。
    """
    markers = cfg.get("markers", [])
    patterns = cfg.get("patterns", [])
    if not markers or not patterns:
        fail("versions/server-side.json 缺 markers 或 patterns")
        return

    seen = 0
    for pattern in patterns:
        hits = sorted(ROOT.glob(pattern))
        if not hits:
            fail(f"server-side.json 的匹配式一个文件都没命中：{pattern} —— 布局变了？")
            continue
        for f in hits:
            if not f.is_file():
                continue
            seen += 1
            text = _strip_comments(f.read_text(encoding="utf-8", errors="replace"))
            for marker in markers:
                if marker in text:
                    fail(f"{f.relative_to(ROOT).as_posix()} 是服务端侧的类，却引用了客户端类型 "
                         f"{marker} —— 专用服务端上会 NoClassDefFoundError")
    minimum = int(cfg.get("min_files", 1))
    if seen < minimum:
        fail(f"服务端侧只扫到 {seen} 个文件（声明的下限 {minimum}）—— 匹配式或布局出问题了")


def check_shared_java_level() -> None:
    """共享层必须能在**最低**的编译级别上编过，否则某个目标才炸。

    shared/ 一份源码进所有目标，而各目标的 java 级别不同（本仓是 17 与 21）。Java 17 上
    「模式匹配 switch」还只是预览特性 —— 用了它的后果不是这里报错，而是 Java 17 那个目标的
    `./gradlew build` 突然红。
    """
    pattern = re.compile(r"^\s*case\s+[A-Za-z_][\w.]*(<[^>]*>)?\s+[a-z_]\w*\s*(->|:)", re.M)
    levels = [t.get("java") for t in entries_of(load_json("versions/targets.json")).values()
              if isinstance(t, dict) and t.get("java")]
    if not levels:
        fail("versions/targets.json 里没有任何目标声明 java 级别")
        return
    lowest = min(int(v) for v in levels)
    if lowest >= 21:
        notes.append(f"所有目标的 java 级别都 ≥ {lowest}，共享层不受 17 的预览特性限制")
        return
    scanned = 0
    for f in sorted((ROOT / "shared" / "src").rglob("*.java")):
        scanned += 1
        text = _strip_comments(f.read_text(encoding="utf-8", errors="replace"))
        for m in pattern.finditer(text):
            line = text[:m.start()].count("\n") + 1
            fail(f"{f.relative_to(ROOT).as_posix()}:{line} 在共享层里用了模式匹配 switch —— "
                 f"java {lowest} 的目标编不过（那边这还是预览特性）。"
                 f"把它挪到映射层/平台层，或在共享层改用 instanceof 链")
    if scanned < 20:
        fail(f"共享层只扫到 {scanned} 个文件 —— 路径或布局变了？")


IMPORT_RE = re.compile(r"^\s*import\s+(static\s+)?([\w.]+)\s*;", re.M)


def _collect_fqcns(*src_roots: pathlib.Path) -> set:
    """src/java 根目录下所有 .java 文件路径对应的 FQCN 集合。"""
    out = set()
    for r in src_roots:
        if not r.is_dir():
            continue
        for p in r.rglob("*.java"):
            rel = p.relative_to(r).as_posix()[:-len(".java")]
            out.add(rel.replace("/", "."))
    return out


def _resolve(imported: str, index: set) -> str | None:
    """import 目标可能是嵌套类（Outer.Inner）或静态成员（Outer.Inner.member）。
    从完整路径逐步丢尾段，直到命中某个已存在的 FQCN。"""
    parts = imported.split(".")
    while parts:
        candidate = ".".join(parts)
        if candidate in index:
            return candidate
        parts.pop()
    return None


def check_dependency_direction() -> None:
    """共享层的依赖方向：shared/ 只许引 shared/；layers/ 只许引 shared/layers，
    或【本层映射家族的全部在建目标同路径都有】的类。

    「家族全部在建目标都有」是今天的既成事实（层里十几个类引用 ChatBubbleConfig /
    ChatMessageStore 这类平台配置与存储，靠同家族各端同名才编得过；层只挂载在该家族上，
    另一个映射家族跑的是它自己的孪生副本）：把事实编码成闸，任一端改名/搬走立刻红。
    彻底解耦要等 facade 化（AtomChat 的 Platform/Net/Host 那条路线），到时把本检查
    收紧成「只许引 shared/layers」。"""
    shared_main = ROOT / "shared/src/main/java"
    shared_test = ROOT / "shared/src/test/java"
    shared_fqcns = _collect_fqcns(shared_main, shared_test)
    layers = entries_of(load_json("versions/layers.json"))
    layer_fqcns = set()
    layers_root = ROOT / "layers"
    if layers_root.is_dir():
        for axis_dir in layers_root.iterdir():
            if not axis_dir.is_dir():
                continue
            for layer in axis_dir.iterdir():
                if layer.is_dir():
                    layer_fqcns |= _collect_fqcns(layer / "src/main/java", layer / "src/test/java")

    built_platforms = []  # (target_name, mappings_family, FQCN set) 按矩阵声明顺序
    targets = entries_of(load_json("versions/targets.json"))
    for name, t in targets.items():
        if isinstance(t, dict) and (ROOT / t.get("project", "")).is_dir():
            built_platforms.append((name, t.get("mappings"), _collect_fqcns(ROOT / t["project"] / "src/main/java")))

    # 各映射家族的「家族内全部在建目标」交集 —— 映射层的类只挂载在该家族的目标上，
    # 所以家族内同名即可，不必要求另一映射家族也有（那边跑的是孪生副本）。
    family_sets: dict = {}
    for _name, fam, fqcns in built_platforms:
        family_sets.setdefault(fam, []).append(fqcns)
    family_everywhere = {fam: set.intersection(*sets) for fam, sets in family_sets.items() if sets}
    everywhere = set.intersection(*[s for _n, _f, s in built_platforms]) if built_platforms else set()

    def scan(base: pathlib.Path, allowed: set, label: str) -> None:
        if not base.is_dir():
            return
        for f in sorted(base.rglob("*.java")):
            text = f.read_text(encoding="utf-8", errors="replace")
            for m in IMPORT_RE.finditer(text):
                if not m.group(2).startswith(OWN_PREFIX):
                    continue
                if _resolve(m.group(2), allowed) is None:
                    fail(f"{f.relative_to(ROOT).as_posix()} 引用了 {m.group(2)} —— "
                         f"{label}的依赖方向不允许：{label}只许引{'共享层内部的类' if label == 'shared/' else ' shared//layers/ 或本层映射家族全部在建目标都有的类'}")

    scan(shared_main, shared_fqcns, "shared/")
    scan(shared_test, shared_fqcns, "shared/")
    if layers_root.is_dir():
        for axis_dir in layers_root.iterdir():
            if not axis_dir.is_dir():
                continue
            for layer_name in axis_dir.iterdir():
                if not layer_name.is_dir():
                    continue
                decl = layers.get(layer_name.name, {})
                fam = decl.get("mappings")
                allowed = shared_fqcns | layer_fqcns | (family_everywhere.get(fam, everywhere) if fam else everywhere)
                scan(layer_name / "src/main/java", allowed, "layers/")
                scan(layer_name / "src/test/java", allowed, "layers/")


def check_shared_dependencies(apis: dict) -> None:
    """共享层与各层里出现的第三方包，必须落在声明表内。

    这不是白名单制度，而是一张「我确实依赖了这个」的声明表：漏声明的表现是某个目标没带上
    那个库 —— 编得过、跑起来 NoClassDefFoundError。原版与加载器提供的包单列一档（不必声明），
    测试框架也单列（不进产物）。自有包（com.niuqu.chatbubble.*）由依赖方向检查管，不归这里。
    """
    declared = [k for k in apis if not k.startswith("_")]
    provided = [_root_of(line) for line in apis.get("_provided", []) if " — " in line]
    test_only = [_root_of(line) for line in apis.get("_test_only", []) if " — " in line]
    allowed = declared + provided
    if not declared and not provided:
        fail("versions/third-party-apis.json 里一个第三方包都没声明")
        return

    for base in ("shared/src", "layers"):
        basep = ROOT / base
        if not basep.is_dir():
            continue
        for f in sorted(basep.rglob("*.java")):
            is_test = "/test/" in f.as_posix()
            ok_prefixes = list(allowed) + (test_only if is_test else [])
            for m in IMPORT_RE.finditer(f.read_text(encoding="utf-8", errors="replace")):
                pkg = m.group(2)
                if pkg.startswith(("java.", "javax.", OWN_PREFIX)):
                    continue
                if any(pkg == pre or pkg.startswith(pre + ".") for pre in ok_prefixes):
                    continue
                fail(f"{f.relative_to(ROOT).as_posix()} 引用了未声明的第三方包 {pkg} —— "
                     f"把它写进 versions/third-party-apis.json（_provided 那一档是原版/加载器提供的）")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix-out", help="把 CI 矩阵写成 JSON 到这个路径")
    args = parser.parse_args()

    targets_raw = load_json("versions/targets.json")
    layers_raw = load_json("versions/layers.json")
    loaders_raw = load_json("versions/loaders.json")
    paths_raw = load_json("versions/resource-paths.json")
    server_raw = load_json("versions/server-side.json")
    pinned_raw = load_json("versions/pinned-equal.json")
    targets = entries_of(targets_raw)
    layers = entries_of(layers_raw)
    loaders = [x for x in (loaders_raw.get("loaders") or []) if isinstance(x, str)]

    check_root_identity()
    check_layers(layers)
    check_resource_paths(paths_raw)
    check_shared_dependencies(load_json("versions/third-party-apis.json"))
    check_shared_java_level()
    check_server_side(server_raw)
    check_pinned_equal(pinned_raw)
    check_dependency_direction()
    check_targets(targets, layers, loaders)

    # 摘要（CI 日志里看这一份）
    print(f"目标 {len(targets)} 条：")
    for name, t in targets.items():
        if not isinstance(t, dict) or "project" not in t:
            continue
        mark = "发" if t.get("buildable") else "不发"
        exists = "有工程" if (ROOT / t["project"]).is_dir() else "无工程"
        print(f"  {name:18} mc={t.get('minecraft'):7} loader={t.get('loader'):9} "
              f"java={t.get('java')} gradle={t.get('gradle')} {mark}/{exists} layers={t.get('layers')}")

    if errors:
        print("\n校验未通过：", file=sys.stderr)
        for e in errors:
            print(f"  ✘ {e}", file=sys.stderr)
        return 1

    print("\n校验通过。")
    for n in notes:
        print(f"  · {n}")

    if args.matrix_out:
        # 产物名后缀在这里算好，不交给工作流里的表达式：
        # GitHub 表达式的 `a && '' || b` 因为空串是 falsy 会静默取到 b，
        # 于是"没验证"的标记会贴到正式产物上（AtomChat 踩过一次）。
        matrix = [
            {
                "target": name,
                "project": t["project"],
                "java": t["java"],
                "buildable": bool(t["buildable"]),
                "suffix": "" if t["buildable"] else "-未验证",
                # 商店版本号的组成段（发布后校验闸用）：{loader}-{mc}-{mod_version}
                "loader": t["loader"],
                "minecraft": t["minecraft"],
            }
            for name, t in targets.items()
            if isinstance(t, dict) and (ROOT / t.get("project", "")).is_dir()
        ]
        matrix.sort(key=lambda e: e["target"])
        with open(args.matrix_out, "w", encoding="utf-8") as f:
            json.dump(matrix, f, ensure_ascii=False)
        print(f"\nCI 矩阵（{len(matrix)} 个目标）已写入 {args.matrix_out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
