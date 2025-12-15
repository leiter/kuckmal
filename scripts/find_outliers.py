#!/usr/bin/env python3
"""
Find outlier entries in Filmliste-akt
"""

import json
import re
from collections import defaultdict

def parse_duration(duration_str):
    """Parse duration string HH:MM:SS to seconds"""
    if not duration_str:
        return 0
    parts = duration_str.split(':')
    if len(parts) == 3:
        return int(parts[0]) * 3600 + int(parts[1]) * 60 + int(parts[2])
    return 0

def analyze_outliers(filepath):
    """Find outlier entries"""
    print(f"Analyzing {filepath} for outliers...\n")

    field_names = [
        "channel", "theme", "title", "date", "time", "duration", "size",
        "description", "url", "website", "subtitle_url", "rtmp_url",
        "small_url", "rtmp_small_url", "hd_url", "rtmp_hd_url",
        "timestamp", "history_url", "geo", "is_new"
    ]

    durations = []
    sizes = []
    entries_data = []
    theme_counts = defaultdict(int)
    channel_counts = defaultdict(int)

    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

        x_pattern = r'"X":\[(.*?)\](?=,\s*["}])'
        matches = re.finditer(x_pattern, content, re.DOTALL)

        current_channel = ""
        current_theme = ""

        for match in matches:
            try:
                entry_str = "[" + match.group(1) + "]"
                entry = json.loads(entry_str)

                # Update current channel/theme
                if entry[0]:
                    current_channel = entry[0]
                if entry[1]:
                    current_theme = entry[1]

                # Count themes and channels
                if current_theme:
                    theme_counts[current_theme] += 1
                if current_channel:
                    channel_counts[current_channel] += 1

                # Parse duration and size
                duration_seconds = parse_duration(entry[5] if len(entry) > 5 else "")
                size_mb = int(entry[6]) if len(entry) > 6 and entry[6] else 0

                # Count non-empty fields
                non_empty_fields = sum(1 for field in entry if field)

                # Store entry data
                entry_data = {
                    'entry': entry,
                    'current_channel': current_channel,
                    'current_theme': current_theme,
                    'duration_seconds': duration_seconds,
                    'size_mb': size_mb,
                    'non_empty_fields': non_empty_fields
                }

                entries_data.append(entry_data)

                if duration_seconds > 0:
                    durations.append(duration_seconds)
                if size_mb > 0:
                    sizes.append(size_mb)

            except:
                pass

    print(f"Total entries analyzed: {len(entries_data):,}\n")
    print("="*70)

    # Find outliers

    # 1. Longest duration
    longest = max(entries_data, key=lambda x: x['duration_seconds'])
    print("OUTLIER #1: LONGEST DURATION")
    print("="*70)
    display_entry(longest, field_names)

    # 2. Shortest duration (but not zero)
    non_zero_durations = [e for e in entries_data if e['duration_seconds'] > 0]
    shortest = min(non_zero_durations, key=lambda x: x['duration_seconds'])
    print("\nOUTLIER #2: SHORTEST DURATION (non-zero)")
    print("="*70)
    display_entry(shortest, field_names)

    # 3. Rarest theme (with at least some entries to be interesting)
    # Find themes with exactly 1 entry
    rare_themes = [theme for theme, count in theme_counts.items() if count == 1]
    if rare_themes:
        # Find an entry with one of these rare themes
        rare_entry = None
        for entry_data in entries_data:
            if entry_data['current_theme'] in rare_themes:
                rare_entry = entry_data
                break

        if rare_entry:
            print("\nOUTLIER #3: UNIQUE THEME (only 1 entry in entire dataset)")
            print("="*70)
            display_entry(rare_entry, field_names)

    # 4. Largest file size
    largest = max(entries_data, key=lambda x: x['size_mb'])
    print("\nOUTLIER #4: LARGEST FILE SIZE")
    print("="*70)
    display_entry(largest, field_names)

    # 5. Most complete entry (most non-empty fields)
    most_complete = max(entries_data, key=lambda x: x['non_empty_fields'])
    print("\nOUTLIER #5: MOST COMPLETE (most fields filled)")
    print("="*70)
    display_entry(most_complete, field_names)

    # 6. Most sparse entry (least non-empty fields, but not completely empty)
    non_empty_entries = [e for e in entries_data if e['non_empty_fields'] > 2]
    most_sparse = min(non_empty_entries, key=lambda x: x['non_empty_fields'])
    print("\nOUTLIER #6: MOST SPARSE (least fields filled)")
    print("="*70)
    display_entry(most_sparse, field_names)

def display_entry(entry_data, field_names):
    """Display a single entry nicely"""
    entry = entry_data['entry']

    print(f"Channel (inherited): {entry_data['current_channel']}")
    print(f"Theme (inherited): {entry_data['current_theme']}")
    print(f"Duration: {entry_data['duration_seconds']} seconds ({entry_data['duration_seconds']//60} min {entry_data['duration_seconds']%60} sec)")
    print(f"Size: {entry_data['size_mb']} MB")
    print(f"Non-empty fields: {entry_data['non_empty_fields']}/{len(field_names)}")
    print()

    for i, field_name in enumerate(field_names):
        if i < len(entry):
            value = entry[i]
            if value:  # Only show non-empty
                # Truncate long values
                if isinstance(value, str) and len(value) > 100:
                    value = value[:97] + "..."
                print(f"  {field_name:20s}: {value}")
    print()

if __name__ == "__main__":
    filepath = "/home/mandroid/Videos/kuckmal-apps/tmp/Filmliste-akt"
    analyze_outliers(filepath)
