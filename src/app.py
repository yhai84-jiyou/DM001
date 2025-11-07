from __future__ import annotations

from itertools import cycle
from pathlib import Path
from typing import Dict, List

from flask import Flask, render_template, url_for

app = Flask(__name__, static_folder="static", template_folder="templates")
IMAGE_DIR = Path(app.static_folder) / "images"
SUPPORTED_EXTENSIONS = {".png", ".jpg", ".jpeg", ".gif", ".webp"}

SAMPLE_METADATA = [
    {"location": "冰岛 · 维克", "description": "黑沙滩与翻涌浪花的长曝光"},
    {"location": "四川 · 亚丁", "description": "雪山脚下的高原湖泊晨光"},
    {"location": "日本 · 奈良", "description": "静谧林间小路与悠闲梅花鹿"},
]

# Ensure the images directory exists even if the user hasn't added their own files yet.
IMAGE_DIR.mkdir(parents=True, exist_ok=True)


def _gather_images() -> List[Dict[str, str]]:
    """Return sorted metadata dictionaries for supported image files."""
    images: List[Dict[str, str]] = []
    metadata_cycle = cycle(SAMPLE_METADATA)
    if IMAGE_DIR.exists():
        for path in sorted(IMAGE_DIR.iterdir()):
            if path.suffix.lower() in SUPPORTED_EXTENSIONS:
                meta = next(metadata_cycle)
                images.append(
                    {
                        "filename": f"images/{path.name}",
                        "location": meta["location"],
                        "description": meta["description"],
                    }
                )
    return images


@app.route("/")
def index() -> str:
    image_cards = [
        {
            "src": url_for("static", filename=img["filename"]),
            "location": img["location"],
            "description": img["description"],
        }
        for img in _gather_images()
    ]
    return render_template("index.html", images=image_cards)


if __name__ == "__main__":
    app.run(debug=True)
