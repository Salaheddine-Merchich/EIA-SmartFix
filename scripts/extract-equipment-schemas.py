#!/usr/bin/env python3
"""
Extract equipment schema pages from manufacturer PDFs, or generate placeholder PNGs for seed.

Usage:
  python scripts/extract-equipment-schemas.py
  python scripts/extract-equipment-schemas.py --pdf-dir "C:/Users/.../Desktop/Data OCp"
"""

from __future__ import annotations

import argparse
import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SEED_DIR = ROOT / "backend" / "src" / "main" / "resources" / "seed" / "equipment-schemas"

# (output_filename, pdf_name, page_0based, label)
EXTRACTIONS = [
    ("veichi-p04-dimension.png", "Manuel-VEICHI-4-KW-SI23-D5-004G.pdf", 3, "VEICHI p4"),
    ("veichi-p05-terminals.png", "Manuel-VEICHI-4-KW-SI23-D5-004G.pdf", 4, "VEICHI p5"),
    ("veichi-p06-wiring-power.png", "Manuel-VEICHI-4-KW-SI23-D5-004G.pdf", 5, "VEICHI p6"),
    ("veichi-p07-wiring-control.png", "Manuel-VEICHI-4-KW-SI23-D5-004G.pdf", 6, "VEICHI p7"),
    ("veichi-p07-sonde-eau.png", "Manuel-VEICHI-4-KW-SI23-D5-004G.pdf", 6, "VEICHI p7 sonde"),
    ("goodrive-p12-install.png", "Manuel-GD-100-FR-1.pdf", 11, "Goodrive p12"),
    ("goodrive-p13-system-pv.png", "Manuel-GD-100-FR-1.pdf", 12, "Goodrive p13"),
    ("goodrive-p13-pompe-pv.png", "Manuel-GD-100-FR-1.pdf", 12, "Goodrive p13 pompe"),
    ("goodrive-p13-cap-pv.png", "Manuel-GD-100-FR-1.pdf", 12, "Goodrive p13 CAP"),
    ("goodrive-p14-terminals.png", "Manuel-GD-100-FR-1.pdf", 13, "Goodrive p14"),
    ("goodrive-p15-motor-terminals.png", "Manuel-GD-100-FR-1.pdf", 14, "Goodrive p15"),
    ("goodrive-p16-digital-io.png", "Manuel-GD-100-FR-1.pdf", 15, "Goodrive p16"),
    ("hitachi-p2-13-dimension.png", "manuel vv hitachi.pdf", 52, "SJ200 p2-13 dimensions"),
    ("hitachi-p2-20-terminals-input.png", "manuel vv hitachi.pdf", 59, "SJ200 p2-20 bornes entree"),
    ("hitachi-p2-23-motor-output.png", "manuel vv hitachi.pdf", 62, "SJ200 p2-23 sortie moteur"),
    ("acs880-p128-zcu12.png", "FR_ACS880-11_HW_H-1.pdf", 127, "ACS880 p128"),
    ("acs880-p218-sto.png", "FR_ACS880-11_HW_H-1.pdf", 217, "ACS880 p218"),
    ("acs880-spin-p30-control.png", "Data ocp.pdf", 29, "ACS880 spin p30"),
    ("acs880-spin-p30-encoder.png", "Data ocp.pdf", 29, "ACS880 spin encoder"),
    ("acs880-spin-p108-brake.png", "Data ocp.pdf", 107, "ACS880 spin p108"),
]


def _png_chunk(tag: bytes, data: bytes) -> bytes:
    crc = zlib.crc32(tag + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)


def placeholder_png(label: str, width: int = 640, height: int = 480) -> bytes:
    """Minimal valid PNG with solid color (no text dependency)."""
    # RGBA flat color based on label hash
    h = sum(ord(c) for c in label) % 200
    row = bytes([40 + h, 80 + (h % 80), 140 + (h % 60), 255]) * width
    raw = b"".join(b"\x00" + row for _ in range(height))
    compressed = zlib.compress(raw, 9)
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    return (
        b"\x89PNG\r\n\x1a\n"
        + _png_chunk(b"IHDR", ihdr)
        + _png_chunk(b"IDAT", compressed)
        + _png_chunk(b"IEND", b"")
    )


def extract_from_pdf(pdf_path: Path, page_index: int, output_path: Path) -> bool:
    try:
        import fitz  # pymupdf
    except ImportError:
        return False

    doc = fitz.open(pdf_path)
    if page_index < 0 or page_index >= len(doc):
        doc.close()
        return False
    page = doc.load_page(page_index)
    pix = page.get_pixmap(matrix=fitz.Matrix(2, 2))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    pix.save(str(output_path))
    doc.close()
    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="Extract or generate equipment schema PNG seeds")
    parser.add_argument(
        "--pdf-dir",
        type=Path,
        default=Path.home() / "Desktop" / "Data OCp",
        help="Directory containing manufacturer PDFs",
    )
    args = parser.parse_args()

    SEED_DIR.mkdir(parents=True, exist_ok=True)
    extracted = 0
    placeholders = 0

    for filename, pdf_name, page, label in EXTRACTIONS:
        out = SEED_DIR / filename
        pdf_path = args.pdf_dir / pdf_name
        if pdf_path.exists() and extract_from_pdf(pdf_path, page, out):
            extracted += 1
            print(f"Extracted {filename} from {pdf_name} p{page + 1}")
        else:
            out.write_bytes(placeholder_png(label))
            placeholders += 1
            print(f"Placeholder {filename} ({label})")

    print(f"Done: {extracted} extracted, {placeholders} placeholders -> {SEED_DIR}")


if __name__ == "__main__":
    main()
