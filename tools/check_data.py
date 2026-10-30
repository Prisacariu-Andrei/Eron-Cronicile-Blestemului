"""Verificatorul datelor jocului (res/Data/*.json). Rulare din rădăcina proiectului:  python tools/check_data.py

Prinde greșelile care strică o prezentare: JSON invalid, scene cu declanșatori greșiți, vorbitori fără portret,
litere cu sedilă (ş ţ în loc de ș ț), replici prea lungi, imagini lipsă, inamici/cărți/NPC-uri inexistente.
Ieșire: o linie pe problemă și totalul; cod 1 dacă există erori (avertismentele nu opresc nimic).
"""
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "res")
DATA = os.path.join(RES, "Data")
SRC = os.path.join(ROOT, "src")
MAX_LINE = 120                      # ~2 rânduri într-o casetă de dialog
CEDILLA = re.compile("[şţŞŢ]")      # trebuie ș ț Ș Ț (virgulă dedesubt)

errors, warnings = [], []
def err(msg): errors.append(msg)
def warn(msg): warnings.append(msg)


def load_all():
    data = {}
    for name in sorted(os.listdir(DATA)):
        if not name.endswith(".json"):
            continue
        path = os.path.join(DATA, name)
        try:
            with open(path, encoding="utf-8") as f:
                data[name] = json.load(f)
        except json.JSONDecodeError as e:
            err(f"{name}: JSON invalid la linia {e.lineno}, coloana {e.colno}: {e.msg}")
    return data


def java_cases(rel):
    try:
        with open(os.path.join(SRC, rel), encoding="utf-8") as f:
            return set(re.findall(r'case\s+"([^"]+)"\s*->', f.read()))
    except OSError:
        warn(f"nu am găsit {rel} (verificarea numelor din cod e sărită)")
        return set()


def res_exists(path):
    """Calea unei resurse ca în cod ("/NPC/Mira" = set de cadre, "/Enemy/X.png" = imagine)."""
    p = path.lstrip("/")
    full = os.path.join(RES, p)
    if os.path.isfile(full):
        return True
    if not os.path.splitext(p)[1]:      # set de cadre: <cale>_down1.png sau <cale>.png
        return os.path.isfile(full + "_down1.png") or os.path.isfile(full + ".png")
    return False


def check_text(where, text):
    if not isinstance(text, str):
        return
    if CEDILLA.search(text):
        err(f"{where}: literă cu sedilă (ş/ţ) în loc de ș/ț: {text[:60]!r}")
    for part in text.split("\n"):
        if len(part) > MAX_LINE:
            warn(f"{where}: replică lungă ({len(part)} caractere > {MAX_LINE}): {part[:50]!r}...")


def walk_texts(where, node, keys=("text", "lines", "afterLines", "closedText", "openText", "quote", "start", "progress", "complete")):
    """Toate textele afișate în joc dintr-un fișier: valorile cheilor de text, oricât de adânc."""
    if isinstance(node, dict):
        for k, v in node.items():
            if k.startswith("_"):
                continue
            if k in keys:
                if isinstance(v, str):
                    check_text(f"{where}.{k}", v)
                elif isinstance(v, list):
                    for i, x in enumerate(v):
                        if isinstance(x, str):
                            check_text(f"{where}.{k}[{i}]", x)
                        else:
                            walk_texts(f"{where}.{k}[{i}]", x, keys)
                else:
                    walk_texts(f"{where}.{k}", v, keys)
            else:
                walk_texts(f"{where}.{k}", v, keys)
    elif isinstance(node, list):
        for i, x in enumerate(node):
            walk_texts(f"{where}[{i}]", x, keys)


PLACEHOLDER = re.compile(r"%(?:\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[a-zA-Z]")   # %s %d %.1f ... (fără %% )


def placeholders(text):
    return sorted(PLACEHOLDER.findall(text.replace("%%", "")))


def check_lang():
    """res/lang/ro.json și en.json: aceleași chei, aceleași locuri pentru valori (%s %d), fără ş ţ, iar fiecare Lang.t("...") din cod există."""
    langs = {}
    for code in ("ro", "en"):
        path = os.path.join(RES, "lang", code + ".json")
        try:
            with open(path, encoding="utf-8") as f:
                raw = json.load(f)
        except OSError:
            err(f"res/lang/{code}.json lipsește")
            continue
        except json.JSONDecodeError as e:
            err(f"res/lang/{code}.json: JSON invalid la linia {e.lineno}, coloana {e.colno}: {e.msg}")
            continue
        langs[code] = {k: v for k, v in raw.items() if not k.startswith("_")}
    if len(langs) < 2:
        return
    ro, en = langs["ro"], langs["en"]
    for k in sorted(set(ro) - set(en)):
        err(f"lang: cheia {k!r} există în ro.json, dar nu în en.json")
    for k in sorted(set(en) - set(ro)):
        err(f"lang: cheia {k!r} există în en.json, dar nu în ro.json")
    for code, table in langs.items():
        for k, v in table.items():
            if not isinstance(v, str) or not v.strip():
                err(f"lang {code}.json: valoare goală sau care nu e text la cheia {k!r}")
                continue
            if CEDILLA.search(v):
                err(f"lang {code}.json: literă cu sedilă (ş/ţ) în loc de ș/ț la cheia {k!r}: {v[:60]!r}")
    for k in sorted(set(ro) & set(en)):
        if isinstance(ro[k], str) and isinstance(en[k], str) and placeholders(ro[k]) != placeholders(en[k]):
            err(f"lang: cheia {k!r} are locuri pentru valori diferite în ro ({placeholders(ro[k])}) și en ({placeholders(en[k])})")
    used = set()
    for base, _dirs, files in os.walk(SRC):
        for fn in files:
            if fn.endswith(".java"):
                with open(os.path.join(base, fn), encoding="utf-8") as f:
                    text = f.read()
                for key in re.findall(r'Lang\.t\(\s*"([^"]+)"\s*[,)]', text):   # doar cheile întregi, nu prefixele compuse în cod
                    if key not in ro:
                        err(f"{fn}: Lang.t(\"{key}\") nu are cheie în res/lang/ro.json")
                used.update(re.findall(r'"([a-z_]+(?:\.[a-z_]+)+)"', text))   # și cheile din tablouri (ex. OPTION_KEYS)
    for k in sorted(set(ro) - used):
        if not k.startswith("settings.lang."):   # se compune în cod: "settings.lang." + codul limbii
            warn(f"lang: cheia {k!r} nu apare în cod")


def main():
    d = load_all()
    check_lang()
    story, gates, npcs = d.get("story.json", {}), d.get("gates.json", {}), d.get("npcs.json", {})
    enemies, castle, intro = d.get("enemies.json", {}), d.get("castle.json", {}), d.get("boss_intro.json", {})
    quests, cards, economy, allies = d.get("quests.json", {}), d.get("cards.json", {}), d.get("economy.json", {}), d.get("allies.json", {})

    # --- texte: sedile și lungime, în toate fișierele cu dialog
    for name in ("story.json", "boss_intro.json", "allies.json", "npcs.json", "quests.json", "gates.json", "castle.json"):
        if name in d:
            walk_texts(name, d[name])

    # --- flag-uri cunoscute
    flags = set(gates.get("bosses", {}).values())
    beast = gates.get("beast", {})
    if isinstance(beast, dict) and beast.get("flag"):
        flags.add(beast["flag"])
    flags.add("fiara")
    for s in story.get("scenes", []):
        for a in s.get("onEnd", []):
            if isinstance(a, str) and a.startswith("set:"):
                flags.add(a[4:])
    zones = {z for z in os.listdir(os.path.join(RES, "Maps"))} if os.path.isdir(os.path.join(RES, "Maps")) else set()

    # --- story.json: declanșatori, id-uri unice, vorbitori
    portraits = {k: v for k, v in story.get("portraits", {}).items() if not k.startswith("_")}
    npc_list = npcs.get("npcs", [])
    npc_with_sprite = {n.get("name") for n in npc_list if n.get("spriteSet")}
    speakers_ok = set(portraits) | npc_with_sprite | {"", "Eron"}
    for name, path in portraits.items():
        if isinstance(path, str) and path and not res_exists(path):
            err(f"story.json portraits[{name}]: resursa lipsește: {path}")
    trig = re.compile(r"^(game_start|epilogue|finale|enter_zone:.+|before_boss:.+|after_boss:.+|flag:.+|inspect:.+)$")
    seen_ids = set()
    for s in story.get("scenes", []):
        sid = s.get("id", "?")
        if sid in seen_ids:
            err(f"story.json: id de scenă duplicat: {sid}")
        seen_ids.add(sid)
        t = s.get("trigger", "")
        if not trig.match(t):
            err(f"story.json scena {sid}: declanșator necunoscut: {t!r}")
        elif t.split(":")[0] in ("after_boss", "before_boss", "flag") and t.split(":", 1)[1] not in flags:
            err(f"story.json scena {sid}: flag-ul {t.split(':', 1)[1]!r} nu există în gates.json -> bosses")
        elif t.startswith("enter_zone:") and zones and t[11:] not in zones:
            err(f"story.json scena {sid}: zona {t[11:]!r} nu există în res/Maps")
        for i, line in enumerate(s.get("lines", [])):
            sp = line.get("speaker", "")
            if sp not in speakers_ok:
                warn(f"story.json scena {sid} replica {i}: vorbitorul {sp!r} nu are portret (story.json -> portraits)")
        for a in s.get("actors", []):
            p = a.get("spriteSet") or a.get("sprite")
            if p and not res_exists(p):
                err(f"story.json scena {sid}: actorul {a.get('id')} are sprite lipsă: {p}")

    # --- numele din cod
    types = enemies.get("types", [])
    type_map = {t.get("id") or t.get("name"): t for t in types} if isinstance(types, list) else dict(types)
    enemy_names = java_cases("Entity/Inamici/EnemyFactory.java") | set(type_map)
    card_ids = java_cases("Entity/Cards/CardFactory.java") | {c.get("id") for c in cards.get("cards", [])}
    boss_keys = set(gates.get("bosses", {}).keys())

    # --- boss_intro.json
    for key, b in intro.get("bosses", {}).items():
        if key not in boss_keys:
            err(f"boss_intro.json: cheia {key!r} nu e un boss din gates.json -> bosses")
        for i, line in enumerate(b.get("lines", [])):
            if line.get("who") not in ("boss", "eron"):
                err(f"boss_intro.json {key} replica {i}: who trebuie să fie 'boss' sau 'eron'")

    # --- castle.json
    for p in castle.get("pretorians", []):
        for name in [p.get("enemy")] + list(p.get("with", [])):
            if name not in enemy_names:
                err(f"castle.json {p.get('id')}: inamicul {name!r} nu există (EnemyFactory / enemies.json)")
    lp = castle.get("malakor", {}).get("lastPage")
    if lp is not None and not (0 < lp.get("lifeBelowPercent", 0) < 100):
        err("castle.json malakor.lastPage.lifeBelowPercent trebuie să fie între 1 și 99")

    # --- enemies.json: sprite-uri
    for name, t in type_map.items():
        for k in ("sprite", "fight", "battle"):
            p = t.get(k) if isinstance(t, dict) else None
            if isinstance(p, str) and p.startswith("/") and not res_exists(p):
                err(f"enemies.json types[{name}].{k}: resursa lipsește: {p}")

    # --- npcs.json: id-uri unice, poziții, sprite-uri, magazin
    ids, spots = set(), {}
    for n in npc_list:
        nid = n.get("id", "?")
        if nid in ids:
            err(f"npcs.json: id duplicat {nid!r}")
        ids.add(nid)
        spot = (n.get("zone"), n.get("col"), n.get("row"))
        if spot in spots:
            err(f"npcs.json: {nid!r} și {spots[spot]!r} stau pe aceeași dală {spot}")
        spots[spot] = nid
        for k in ("spriteSet", "sprite"):
            p = n.get(k)
            if p and not res_exists(p):
                err(f"npcs.json {nid}.{k}: resursa lipsește: {p}")
        if n.get("zone") and zones and n["zone"] not in zones:
            err(f"npcs.json {nid}: zona {n['zone']!r} nu există")
        prices = economy.get("cardPrices", {})
        for item in n.get("shop", {}).get("stock", []):
            if item.startswith("rune:"):
                continue
            if item not in card_ids:
                err(f"npcs.json {nid}: cartea din magazin {item!r} nu există (CardFactory / cards.json)")
            elif item not in prices:
                warn(f"npcs.json {nid}: cartea {item!r} nu are preț în economy.json (se folosește defaultCardPrice)")

    # --- quests.json: cine dă și cine primește misiunile
    for q in quests.get("quests", []):
        for k in ("giver", "turnIn"):
            if q.get(k) and q[k] not in ids:
                err(f"quests.json {q.get('id')}: {k} = {q[k]!r} nu e un NPC din npcs.json")

    # --- allies.json
    for a in allies.get("allies", []):
        if a.get("recruitFrom") and a["recruitFrom"] not in enemy_names and a["recruitFrom"].split(",")[0] not in boss_keys:
            err(f"allies.json {a.get('id')}: recruitFrom {a['recruitFrom']!r} nu e un inamic cunoscut")
        for i, line in enumerate(a.get("scene", [])):
            if line.get("speaker", "") not in {"", "Eron", a.get("name")}:
                warn(f"allies.json {a.get('id')} scena {i}: vorbitorul {line.get('speaker')!r} nu e nici aliatul, nici Eron")

    for w in warnings:
        print("AVERTISMENT  " + w)
    for e in errors:
        print("EROARE       " + e)
    print(f"\n{len(errors)} erori, {len(warnings)} avertismente")
    return 1 if errors else 0


if __name__ == "__main__":
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")   # consola Windows
    sys.exit(main())
