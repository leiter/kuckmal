#!/usr/bin/env python3
"""
Download high-quality TV channel logos
Uses alternative sources and direct SVG files from Wikimedia
"""

import requests
import os
from PIL import Image
from io import BytesIO
import cairosvg

# Target directory for logos
OUTPUT_DIR = "../app/src/main/res/drawable"

# Target width for logos (maintaining aspect ratio)
TARGET_WIDTH = 300

# Channel logos with direct URLs from Wikimedia Commons (full SVG files)
# These are verified working URLs
LOGOS = {
    "_3sat.png": "https://upload.wikimedia.org/wikipedia/commons/a/a5/3sat-Logo.svg",
    "ard.png": "https://upload.wikimedia.org/wikipedia/commons/9/9c/ARD_logo.svg",
    "arte_de.png": "https://upload.wikimedia.org/wikipedia/commons/4/4e/Arte_Logo_2011.svg",
    "arte_fr.png": "https://upload.wikimedia.org/wikipedia/commons/4/4e/Arte_Logo_2011.svg",
    "br.png": "https://upload.wikimedia.org/wikipedia/commons/4/46/BR_Dachmarke.svg",
    "hr.png": "https://upload.wikimedia.org/wikipedia/commons/8/89/HR_Logo.svg",
    "kika.png": "https://upload.wikimedia.org/wikipedia/commons/5/5d/KiKa_logo.svg",
    "mdr.png": "https://upload.wikimedia.org/wikipedia/commons/6/6e/MDR_Logo_2017.svg",
    "ndr.png": "https://upload.wikimedia.org/wikipedia/commons/0/0a/NDR_Dachmarke.svg",
    "orf.png": "https://upload.wikimedia.org/wikipedia/commons/4/43/ORF_logo.svg",
    "phoenix.png": "https://upload.wikimedia.org/wikipedia/commons/7/78/Phoenix_Logo_2018.svg",
    "rbb.png": "https://upload.wikimedia.org/wikipedia/commons/5/52/Rbb_Logo_2017.svg",
    "sr.png": "https://upload.wikimedia.org/wikipedia/commons/c/c5/SR_Dachmarke.svg",
    "srf.png": "https://upload.wikimedia.org/wikipedia/commons/1/15/Schweizer_Radio_und_Fernsehen_Logo.svg",
    "srf_podcast.png": "https://upload.wikimedia.org/wikipedia/commons/1/15/Schweizer_Radio_und_Fernsehen_Logo.svg",
    "swr.png": "https://upload.wikimedia.org/wikipedia/commons/e/e4/SWR_Dachmarke.svg",
    "wdr.png": "https://upload.wikimedia.org/wikipedia/commons/6/6c/WDR_Dachmarke.svg",
    "zdf.png": "https://upload.wikimedia.org/wikipedia/commons/f/f5/ZDF_logo.svg",
    "zdf_tivi.png": "https://upload.wikimedia.org/wikipedia/commons/0/00/ZDFtivi_logo.svg",
}

def download_and_process_logo(filename, url):
    """Download logo from URL and process it for Android"""
    try:
        print(f"Downloading {filename}...")

        # Headers to avoid 403 Forbidden
        headers = {
            'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
        }

        # Download image
        response = requests.get(url, headers=headers, timeout=15)
        response.raise_for_status()

        # Check if it's an SVG
        if url.endswith('.svg'):
            # Convert SVG to PNG using cairosvg
            try:
                png_bytes = cairosvg.svg2png(bytestring=response.content, output_width=TARGET_WIDTH)
                img = Image.open(BytesIO(png_bytes))
            except Exception as svg_error:
                print(f"  SVG conversion failed, trying as raster image: {svg_error}")
                img = Image.open(BytesIO(response.content))
        else:
            # Open as regular image
            img = Image.open(BytesIO(response.content))

        # Convert to RGBA if not already (for transparency)
        if img.mode != 'RGBA':
            img = img.convert('RGBA')

        # If image wasn't resized during SVG conversion, resize it now
        if img.width != TARGET_WIDTH:
            aspect_ratio = img.height / img.width
            new_width = TARGET_WIDTH
            new_height = int(TARGET_WIDTH * aspect_ratio)
            img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

        # Save as PNG with optimization
        output_path = os.path.join(OUTPUT_DIR, filename)
        img.save(output_path, 'PNG', optimize=True)

        print(f"✓ Saved {filename} ({img.width}x{img.height})")
        return True

    except Exception as e:
        print(f"✗ Failed to download {filename}: {e}")
        return False

def main():
    """Download all logos"""
    print("=" * 60)
    print("TV Channel Logo Downloader")
    print("=" * 60)

    # Check if output directory exists
    if not os.path.exists(OUTPUT_DIR):
        print(f"Error: Output directory does not exist: {OUTPUT_DIR}")
        return

    # Check if cairosvg is available
    try:
        import cairosvg
        print("SVG support: Available")
    except ImportError:
        print("SVG support: Not available (install pycairosvg for SVG support)")
        print("Continuing with PNG fallback...")

    print(f"Output directory: {OUTPUT_DIR}")
    print(f"Target width: {TARGET_WIDTH}px\n")

    # Download logos
    success_count = 0
    failed_count = 0

    for filename, url in LOGOS.items():
        if download_and_process_logo(filename, url):
            success_count += 1
        else:
            failed_count += 1

    # Summary
    print("\n" + "=" * 60)
    print(f"Download complete!")
    print(f"Success: {success_count}/{len(LOGOS)}")
    print(f"Failed: {failed_count}/{len(LOGOS)}")
    print("=" * 60)

if __name__ == "__main__":
    main()
