#!/usr/bin/env python3
"""Curate emojis in ARH Whisper Board:
- Keep smileys & emotion
- Keep basic hand gestures (thumbs up/down, clap, peace, wave, salute, pray, etc.) with skin tones
- Keep essential symbols & reactions (fire, heart, 100, checkmark, cross, question, star, etc.)
- Keep categories minimal for animals, food, travel, activities, objects, flags
- Align annotations/en.txt, id.txt, zh.txt exactly with root.txt inventory
"""

import os
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
EMOJI_DIR = ROOT / "app" / "src" / "main" / "assets" / "ime" / "media" / "emoji"
ROOT_TXT = EMOJI_DIR / "root.txt"
ANNOTATIONS_DIR = EMOJI_DIR / "annotations"

# Hand gesture base emojis to keep in people_body
HAND_GESTURES = {
    "👋", "🤚", "🖐️", "✋", "🖖", "🫱", "🫲", "🫳", "🫴", "🫷", "🫸",
    "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙",
    "👈", "👉", "👆", "🖕", "👇", "☝️", "🫵",
    "👍", "👎", "✊", "👊", "🤛", "🤜",
    "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪"
}

# Essential symbols to keep in symbols
ESSENTIAL_SYMBOLS = {
    "❤️", "🩷", "🧡", "💛", "💚", "💙", "🩵", "💜", "🖤", "🩶", "🤍", "🤎",
    "💔", "❤️‍🔥", "❤️‍🩹", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝",
    "💯", "💢", "💥", "💫", "💬", "👁️‍🗨️", "🗨️", "🗯️", "💭", "💤",
    "⚠️", "⛔", "🚫", "✅", "❌", "❓", "❗", "‼️", "⁉️",
    "➕", "➖", "➗", "✖️", "💲", "🔄", "🔁", "🔂", "▶️", "⏸️", "⏹️",
    "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟",
    "🔥", "⭐", "🌟", "✨", "⚡", "💡", "🎉", "🎊", "🚀", "📌", "📍"
}

# Essential animals, food, travel, activities, objects to keep minimal
ESSENTIAL_ANIMALS = {"🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🐢", "🐍", "🦎", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🐊", "🐅", "🐆", "🦓", "🦍", "🦧", "🐘", "🦛", "🦏", "🐪", "🐫", "🦒", "🦘", "🐃", "🐂", "🐄", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🐈", "🐓", "🦃", "🦚", "🦜", "🦢", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦦", "🦥", "🐁", "🐀", "🐿️", "🦔", "🐾", "🐉", "🐲", "🌵", "🎄", "🌲", "🌳", "🌴", "🌱", "🌿", "☘️", "🍀", "🎍", "🎋", "🍃", "🍂", "🍁", "🍄", "🐚", "🌾", "💐", "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌞", "🌝", "🌛", "🌜", "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "💫", "⭐", "🌟", "✨", "⚡", "☄️", "💥", "🔥", "🌪️", "🌈", "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "🌨️", "🌩️", "❄️", "☃️", "⛄", "🌬️", "💨", "💧", "💦", "☔", "☂️", "🌊", "🌫️"}
ESSENTIAL_FOOD = {"🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚", "🍘", "🍢", "🥠", "🥮", "🍧", "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛", "🍼", "🫖", "☕", "🍵", "🧃", "🥤", "🧋", "🍶", "🍺", "🍻", "🥂", "🍷", "🥃", "🍸", "🍹", "🧊"}
ESSENTIAL_TRAVEL = {"🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🦯", "🦽", "🦼", "🛴", "🚲", "🛵", "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟", "🚃", "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆", "🚇", "🚊", "🚉", "✈️", "🛫", "🛬", "🛩️", "💺", "🛰️", "🚀", "🛸", "🚁", "🛶", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢", "⚓", "⛽", "🚧", "🚦", "🚥", "🛑", "🎡", "🎢", "🎪", "🏢", "🏠", "🏡", "🏥", "🏦", "🏨", "🏫", "🏬", "🏭", "🏰", "🗼", "🗽", "🕌", "⛩️", "🌅", "🌄", "🌇", "🌆", "🌃", "🌌", "🌉"}
ESSENTIAL_ACTIVITIES = {"⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳", "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🥌", "🎿", "⛷️", "🏂", "🏋️", "🤼", "🤸", "🤺", "🧗", "🧘", "🏇", "🏄", "🏊", "🤽", "🚣", "🧗‍♀️", "🧗‍♂️", "🏇", "🎮", "🕹️", "🎲", "♟️", "🎯", "🎳", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️"}
ESSENTIAL_OBJECTS = {"📱", "📲", "☎️", "📞", "📟", "📠", "🔋", "🪫", "🔌", "💻", "🖥️", "🖨️", "⌨️", "🖱️", "🖲️", "💽", "💾", "💿", "📀", "📷", "📸", "📹", "🎥", "📽️", "🎞️", "📞", "📺", "📻", "🎙️", "🎚️", "🎛️", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋", "💡", "🔦", "🕯️", "🧯", "🗑️", "🛢️", "💸", "💵", "💴", "💶", "💷", "🪙", "💰", "💳", "💎", "⚖️", "🪜", "🧰", "🪛", "🔧", "🔨", "⚒️", "🛠️", "⛏️", "🪚", "🔩", "⚙️", "🪤", "🧱", "⛓️", "🧲", "🔫", "💣", "🧨", "🪓", "🔪", "🗡️", "⚔️", "🛡️", "🚬", "⚰️", "🪦", "⚱️", "🏺", "🔮", "📿", "🧿", "🪬", "💈", "⚗️", "🔭", "🔬", "🕳️", "🩹", "🩺", "💊", "💉", "🩸", "🧬", "🦠", "🧫", "🧪", "🌡️", "🧹", "🪠", "🧺", "🧻", "🚽", "🚰", "🚿", "🛁", "🛀", "🧼", "🪥", "🪒", "🧽", "🪣", "🧴", "🔑", "🗝️", "🚪", "🪑", "🛋️", "🛏️", "🛌", "🖼️", "🪞", "🪟", "🛍️", "🛒", "🎁", "🎈", "🎏", "🎀", "🪄", "🪅", "🎊", "🎉", "🎎", "🏮", "🎐", "🧧", "✉️", "📩", "📨", "📧", "💌", "📮", "📦", "🏷️", "🪧", "📫", "📪", "📬", "📭", "📜", "📃", "📄", "📑", "🧾", "📊", "📈", "📉", "🗒️", "🗓️", "📆", "📅", "📇", "🗃️", "🗳️", "🗄️", "📋", "📁", "📂", "🗂️", "🗞️", "📰", "📓", "📕", "📗", "📘", "📙", "📚", "📖", "🔖", "🧷", "🔗", "📎", "🖇️", "📐", "📏", "🧮", "📌", "📍", "✂️", "🖊️", "🖋️", "✒️", "🖌️", "🖍️", "📝", "✏️", "🔍", "🔎", "🔏", "🔐", "🔒", "🔓"}
ESSENTIAL_FLAGS = {"🏁", "🚩", "🎌", "🏴", "🏳️"}

def curate():
    lines = ROOT_TXT.read_text(encoding="utf-8").splitlines()
    
    output_lines = ["# Curated Lean Emoji Set for ARH Whisper Board", "#", ""]
    current_cat = None
    keep_block = False
    curated_inventory = set()
    
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("#"):
            i += 1
            continue
        if line.startswith("["):
            current_cat = line.strip("[]")
            output_lines.append(line)
            i += 1
            continue
        if not line.strip():
            i += 1
            continue
            
        # Base emoji line
        if not line.startswith("\t"):
            base_emoji = line.split(";")[0].strip()
            # Decide whether to keep
            should_keep = False
            if current_cat == "smileys_emotion":
                should_keep = True
            elif current_cat == "people_body":
                should_keep = base_emoji in HAND_GESTURES
            elif current_cat == "symbols":
                should_keep = base_emoji in ESSENTIAL_SYMBOLS
            elif current_cat == "animals_nature":
                should_keep = base_emoji in ESSENTIAL_ANIMALS
            elif current_cat == "food_drink":
                should_keep = base_emoji in ESSENTIAL_FOOD
            elif current_cat == "travel_places":
                should_keep = base_emoji in ESSENTIAL_TRAVEL
            elif current_cat == "activities":
                should_keep = base_emoji in ESSENTIAL_ACTIVITIES
            elif current_cat == "objects":
                should_keep = base_emoji in ESSENTIAL_OBJECTS
            elif current_cat == "flags":
                should_keep = base_emoji in ESSENTIAL_FLAGS
                
            if should_keep:
                output_lines.append(line)
                curated_inventory.add(base_emoji)
                keep_block = True
            else:
                keep_block = False
            i += 1
        else:
            # Sub-variant (skin tone)
            if keep_block:
                output_lines.append(line)
            i += 1

    ROOT_TXT.write_text("\n".join(output_lines) + "\n", encoding="utf-8")
    print(f"Curated root.txt: {len(curated_inventory)} base emojis, {len(output_lines)} lines.")
    
    # Now align annotations
    for lang in ["en", "id", "zh"]:
        ann_file = ANNOTATIONS_DIR / f"{lang}.txt"
        if not ann_file.is_file():
            continue
        ann_lines = ann_file.read_text(encoding="utf-8").splitlines()
        filtered = []
        for l in ann_lines:
            if not l.strip():
                continue
            emoji = l.split(";")[0].strip()
            if emoji in curated_inventory:
                filtered.append(l)
        ann_file.write_text("\n".join(filtered) + "\n", encoding="utf-8")
        print(f"Filtered {lang}.txt: {len(filtered)} annotations.")

if __name__ == "__main__":
    curate()
