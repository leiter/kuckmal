#!/usr/bin/env python3
"""
Process downloaded TV channel logos:
- Resize to optimal size for Android
- Optimize file size
- Rename correctly
- Move to drawable folder
"""

import os
from PIL import Image
import shutil

# Directories
INPUT_DIR = "./downloaded_logos"  # Put your downloaded logos here
OUTPUT_DIR = "../app/src/main/res/drawable"

# Target width (maintains aspect ratio)
TARGET_WIDTH = 300

# Expected filenames (will auto-detect and rename if needed)
EXPECTED_NAMES = {
    "3sat": "_3sat.png",
    "ard": "ard.png",
    "arte": "arte_de.png",  # Will duplicate for arte_fr
    "br": "br.png",
    "hr": "hr.png",
    "kika": "kika.png",
    "mdr": "mdr.png",
    "ndr": "ndr.png",
    "orf": "orf.png",
    "phoenix": "phoenix.png",
    "rbb": "rbb.png",
    "sr": "sr.png",
    "srf": "srf.png",  # Will duplicate for srf_podcast
    "swr": "swr.png",
    "wdr": "wdr.png",
    "zdf": "zdf.png",
    "tivi": "zdf_tivi.png",
}

def find_logo_file(channel_name):
    """Find logo file in input directory by searching for channel name"""
    if not os.path.exists(INPUT_DIR):
        return None

    # Search for files containing channel name (case-insensitive)
    for filename in os.listdir(INPUT_DIR):
        if filename.lower().endswith(('.png', '.jpg', '.jpeg', '.svg', '.webp')):
            if channel_name.lower() in filename.lower():
                return os.path.join(INPUT_DIR, filename)
    return None

def process_logo(input_path, output_filename):
    """Resize and optimize a logo"""
    try:
        print(f"Processing {os.path.basename(input_path)} → {output_filename}...")

        # Open image
        img = Image.open(input_path)

        # Convert to RGBA for transparency support
        if img.mode != 'RGBA':
            # If it's a mode with transparency, preserve it
            if img.mode in ('LA', 'P'):
                img = img.convert('RGBA')
            else:
                # For RGB, just convert
                img = img.convert('RGBA')

        # Resize maintaining aspect ratio
        if img.width != TARGET_WIDTH:
            aspect_ratio = img.height / img.width
            new_width = TARGET_WIDTH
            new_height = int(TARGET_WIDTH * aspect_ratio)
            img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)

        # Save as optimized PNG
        output_path = os.path.join(OUTPUT_DIR, output_filename)
        img.save(output_path, 'PNG', optimize=True)

        file_size = os.path.getsize(output_path) / 1024  # KB
        print(f"✓ Saved {output_filename} ({img.width}x{img.height}, {file_size:.1f} KB)")
        return True

    except Exception as e:
        print(f"✗ Failed to process {output_filename}: {e}")
        return False

def main():
    print("=" * 60)
    print("TV Channel Logo Processor")
    print("=" * 60)

    # Check directories
    if not os.path.exists(INPUT_DIR):
        print(f"\nCreating input directory: {INPUT_DIR}")
        os.makedirs(INPUT_DIR)
        print(f"\nPlease download logos to: {INPUT_DIR}")
        print("Then run this script again.")
        return

    if not os.path.exists(OUTPUT_DIR):
        print(f"Error: Output directory not found: {OUTPUT_DIR}")
        return

    print(f"Input directory: {INPUT_DIR}")
    print(f"Output directory: {OUTPUT_DIR}")
    print(f"Target width: {TARGET_WIDTH}px\n")

    # Count available logos
    input_files = [f for f in os.listdir(INPUT_DIR) if f.lower().endswith(('.png', '.jpg', '.jpeg', '.svg', '.webp'))]
    print(f"Found {len(input_files)} logo file(s) in input directory\n")

    if len(input_files) == 0:
        print("No logo files found. Please download logos to the input directory.")
        return

    # Process each channel
    success_count = 0
    failed_count = 0

    for channel_name, output_filename in EXPECTED_NAMES.items():
        input_path = find_logo_file(channel_name)

        if input_path:
            if process_logo(input_path, output_filename):
                success_count += 1

                # Special cases: duplicate for ARTE and SRF
                if channel_name == "arte":
                    # Copy for ARTE.FR
                    shutil.copy2(
                        os.path.join(OUTPUT_DIR, output_filename),
                        os.path.join(OUTPUT_DIR, "arte_fr.png")
                    )
                    print(f"  → Duplicated as arte_fr.png")

                elif channel_name == "srf":
                    # Copy for SRF Podcast
                    shutil.copy2(
                        os.path.join(OUTPUT_DIR, output_filename),
                        os.path.join(OUTPUT_DIR, "srf_podcast.png")
                    )
                    print(f"  → Duplicated as srf_podcast.png")
            else:
                failed_count += 1
        else:
            print(f"⚠ Skipped {output_filename}: No matching file found for '{channel_name}'")
            failed_count += 1

    # Summary
    print("\n" + "=" * 60)
    print("Processing complete!")
    print(f"Success: {success_count}/{len(EXPECTED_NAMES)}")
    print(f"Failed/Skipped: {failed_count}/{len(EXPECTED_NAMES)}")
    print("=" * 60)

    if failed_count > 0:
        print("\nTip: Make sure logo filenames contain the channel name")
        print("Example: '3sat-logo.png', 'ard_2024.svg', etc.")

if __name__ == "__main__":
    main()
