"""XZ decompression and JSON parsing service for Filmliste format."""

import lzma
import json
import re
from typing import List, Callable, Optional
from sqlalchemy.orm import Session
from sqlalchemy.dialects.sqlite import insert

from models import MediaEntry
from config import PARSER_BATCH_SIZE


class ParserService:
    """Service for parsing Filmliste files."""

    def __init__(self):
        self.status = {
            "state": "idle",
            "progress": 0,
            "entries_parsed": 0,
            "message": ""
        }
        self.should_cancel = False

    def decompress_xz(self, xz_path: str, output_path: str) -> bool:
        """Decompress .xz file using Python's lzma module."""
        self.status = {
            "state": "decompressing",
            "progress": 0,
            "entries_parsed": 0,
            "message": "Decompressing file..."
        }

        try:
            with lzma.open(xz_path, 'rb') as f_in:
                with open(output_path, 'wb') as f_out:
                    while True:
                        chunk = f_in.read(8192)
                        if not chunk:
                            break
                        f_out.write(chunk)
                        if self.should_cancel:
                            return False

            self.status["state"] = "decompress_complete"
            self.status["message"] = "Decompression complete"
            return True

        except lzma.LZMAError as e:
            self.status = {
                "state": "error",
                "progress": 0,
                "entries_parsed": 0,
                "message": f"Decompression error: {str(e)}"
            }
            return False
        except IOError as e:
            self.status = {
                "state": "error",
                "progress": 0,
                "entries_parsed": 0,
                "message": f"File error: {str(e)}"
            }
            return False

    def parse_and_import(
        self,
        json_path: str,
        db_session: Session,
        progress_callback: Optional[Callable[[int], None]] = None,
        is_diff: bool = False
    ) -> int:
        """
        Parse Filmliste JSON and import to SQLite.

        The JSON format uses "X" arrays for entries with field inheritance
        where empty fields inherit from the previous entry.
        """
        self.status = {
            "state": "parsing",
            "progress": 0,
            "entries_parsed": 0,
            "message": "Parsing entries..."
        }
        self.should_cancel = False

        try:
            with open(json_path, 'r', encoding='utf-8') as f:
                content = f.read()

            entries = []
            prev_channel = ""
            prev_theme = ""
            total_imported = 0

            # Find all "X" arrays using regex
            pattern = r'"X"\s*:\s*\[(.*?)\]'
            matches = re.finditer(pattern, content, re.DOTALL)

            for match in matches:
                if self.should_cancel:
                    break

                arr_content = match.group(1)
                values = self._parse_array_values(arr_content)

                if len(values) < 3:
                    continue

                # Apply inheritance for channel/theme
                channel = values[0] if values[0] else prev_channel
                theme = values[1] if values[1] else prev_theme
                prev_channel, prev_theme = channel, theme

                # Skip if no channel or theme
                if not channel or not theme:
                    continue

                # Parse timestamp
                timestamp = 0
                if len(values) > 16 and values[16]:
                    try:
                        timestamp = int(values[16])
                    except ValueError:
                        pass

                # Parse isNew
                is_new = False
                if len(values) > 19 and values[19]:
                    is_new = values[19].lower() == "true"

                entry_data = {
                    "channel": channel,
                    "theme": theme,
                    "title": values[2] if len(values) > 2 else "",
                    "date": values[3] if len(values) > 3 else "",
                    "time": values[4] if len(values) > 4 else "",
                    "duration": values[5] if len(values) > 5 else "",
                    "sizeMB": values[6] if len(values) > 6 else "",
                    "description": values[7] if len(values) > 7 else "",
                    "url": values[8] if len(values) > 8 else "",
                    "website": values[9] if len(values) > 9 else "",
                    "subtitleUrl": values[10] if len(values) > 10 else "",
                    "smallUrl": values[12] if len(values) > 12 else "",
                    "hdUrl": values[14] if len(values) > 14 else "",
                    "timestamp": timestamp,
                    "geo": values[18] if len(values) > 18 else "",
                    "isNew": is_new
                }
                entries.append(entry_data)

                # Batch insert
                if len(entries) >= PARSER_BATCH_SIZE:
                    self._insert_batch(db_session, entries, is_diff)
                    total_imported += len(entries)
                    entries = []

                    self.status["entries_parsed"] = total_imported
                    self.status["message"] = f"Imported {total_imported} entries..."

                    if progress_callback:
                        progress_callback(total_imported)

            # Insert remaining entries
            if entries:
                self._insert_batch(db_session, entries, is_diff)
                total_imported += len(entries)

            self.status = {
                "state": "complete",
                "progress": 100,
                "entries_parsed": total_imported,
                "message": f"Import complete: {total_imported} entries"
            }

            return total_imported

        except (IOError, json.JSONDecodeError) as e:
            self.status = {
                "state": "error",
                "progress": 0,
                "entries_parsed": 0,
                "message": f"Parse error: {str(e)}"
            }
            return 0

    def _parse_array_values(self, arr_content: str) -> List[str]:
        """Parse JSON array string values, handling escaped quotes."""
        values = []
        i = 0
        arr_len = len(arr_content)

        while i < arr_len:
            # Skip whitespace and commas
            while i < arr_len and arr_content[i] in ' \t\n\r,':
                i += 1

            if i >= arr_len:
                break

            if arr_content[i] == '"':
                # Parse quoted string
                i += 1
                start = i
                while i < arr_len:
                    if arr_content[i] == '\\' and i + 1 < arr_len:
                        i += 2  # Skip escaped character
                    elif arr_content[i] == '"':
                        break
                    else:
                        i += 1

                value = arr_content[start:i]
                # Unescape the string
                value = value.replace('\\"', '"').replace('\\\\', '\\')
                values.append(value)
                i += 1  # Skip closing quote
            else:
                # Skip non-string values
                while i < arr_len and arr_content[i] not in ',]':
                    i += 1
                values.append("")

        return values

    def _insert_batch(self, db_session: Session, entries: List[dict], is_diff: bool):
        """Insert batch of entries with upsert to handle duplicates."""
        # Always use INSERT OR REPLACE to handle duplicates in the film list
        for entry_data in entries:
            stmt = insert(MediaEntry).values(**entry_data)
            if is_diff:
                # For diff: update existing entries
                stmt = stmt.on_conflict_do_update(
                    index_elements=['channel', 'theme', 'title'],
                    set_={
                        'date': entry_data['date'],
                        'time': entry_data['time'],
                        'duration': entry_data['duration'],
                        'sizeMB': entry_data['sizeMB'],
                        'description': entry_data['description'],
                        'url': entry_data['url'],
                        'website': entry_data['website'],
                        'subtitleUrl': entry_data['subtitleUrl'],
                        'smallUrl': entry_data['smallUrl'],
                        'hdUrl': entry_data['hdUrl'],
                        'timestamp': entry_data['timestamp'],
                        'geo': entry_data['geo'],
                        'isNew': entry_data['isNew']
                    }
                )
            else:
                # For full import: ignore duplicates (keep first occurrence)
                stmt = stmt.on_conflict_do_nothing(
                    index_elements=['channel', 'theme', 'title']
                )
            db_session.execute(stmt)

        db_session.commit()

    def cancel(self):
        """Cancel ongoing operation."""
        self.should_cancel = True

    def get_status(self) -> dict:
        """Get current parser status."""
        return self.status.copy()
