#!/usr/bin/env python3
"""
Analyze Filmliste-akt to extract all available fields and unique themes/categories
"""

import json
import sys
from collections import Counter

def analyze_filmliste(filepath):
    """Parse and analyze the Filmliste-akt file"""
    print(f"Analyzing {filepath}...")
    print("This may take a while for large files...\n")

    themes = Counter()
    channels = Counter()
    sample_entries = []
    total_entries = 0

    # Field names based on the format
    field_names = [
        "channel", "theme", "title", "date", "time", "duration", "size",
        "description", "url", "website", "subtitle_url", "rtmp_url",
        "small_url", "rtmp_small_url", "hd_url", "rtmp_hd_url",
        "timestamp", "history_url", "geo", "is_new"
    ]

    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            # Parse JSON - handle the special format with multiple "X" keys
            content = f.read()

            # The format uses "X" as a repeated key, which isn't valid JSON
            # We need to parse it specially
            print("Parsing JSON structure...")

            # Find all entries
            import re

            # Find the Filmliste header
            header_match = re.search(r'"Filmliste":\[(.*?)\]', content)
            if header_match:
                print(f"Header: {header_match.group(1)}\n")

            # Find all X entries using regex
            x_pattern = r'"X":\[(.*?)\](?=,\s*["}])'
            matches = re.finditer(x_pattern, content, re.DOTALL)

            current_channel = ""
            current_theme = ""

            for match in matches:
                try:
                    entry_str = "[" + match.group(1) + "]"
                    entry = json.loads(entry_str)

                    # Handle empty fields - inherit from previous entry
                    if entry[0]:  # channel
                        current_channel = entry[0]
                    if entry[1]:  # theme
                        current_theme = entry[1]

                    # Count themes and channels
                    if current_theme:
                        themes[current_theme] += 1
                    if current_channel:
                        channels[current_channel] += 1

                    # Save first 10 entries as samples
                    if len(sample_entries) < 10:
                        entry_dict = {}
                        for i, field_name in enumerate(field_names):
                            if i < len(entry):
                                entry_dict[field_name] = entry[i]
                        entry_dict['_inherited_channel'] = current_channel
                        entry_dict['_inherited_theme'] = current_theme
                        sample_entries.append(entry_dict)

                    total_entries += 1

                    if total_entries % 10000 == 0:
                        print(f"Processed {total_entries} entries...", end='\r')

                except json.JSONDecodeError as e:
                    # Skip malformed entries
                    pass

            print(f"\n\n{'='*70}")
            print(f"ANALYSIS RESULTS")
            print(f"{'='*70}\n")

            print(f"Total entries: {total_entries:,}\n")

            print(f"Available fields ({len(field_names)}):")
            for i, field in enumerate(field_names):
                print(f"  {i:2d}. {field}")

            print(f"\n{'='*70}")
            print(f"UNIQUE THEMES/CATEGORIES ({len(themes)})")
            print(f"{'='*70}\n")

            # Sort themes by frequency
            sorted_themes = sorted(themes.items(), key=lambda x: x[1], reverse=True)

            for theme, count in sorted_themes:
                print(f"  {theme:40s} : {count:6,} entries")

            print(f"\n{'='*70}")
            print(f"UNIQUE CHANNELS ({len(channels)})")
            print(f"{'='*70}\n")

            sorted_channels = sorted(channels.items(), key=lambda x: x[1], reverse=True)
            for channel, count in sorted_channels[:20]:  # Show top 20
                print(f"  {channel:30s} : {count:6,} entries")

            if len(sorted_channels) > 20:
                print(f"  ... and {len(sorted_channels) - 20} more channels")

            print(f"\n{'='*70}")
            print(f"SAMPLE ENTRIES (first 3)")
            print(f"{'='*70}\n")

            for i, entry in enumerate(sample_entries[:3]):
                print(f"Entry {i+1}:")
                for key, value in entry.items():
                    if value:  # Only show non-empty fields
                        print(f"  {key:20s}: {value}")
                print()

    except FileNotFoundError:
        print(f"Error: File not found: {filepath}")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    filepath = "/home/mandroid/Videos/andriod-mediathekview-code/tmp/Filmliste-akt"

    if len(sys.argv) > 1:
        filepath = sys.argv[1]

    analyze_filmliste(filepath)
