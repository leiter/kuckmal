"""Business logic for media operations."""

from typing import List, Optional, Tuple
from sqlalchemy import func, distinct
from sqlalchemy.orm import Session

from models import MediaEntry
from config import DEFAULT_LIMIT, DEFAULT_OFFSET, MAX_LIMIT


class MediaService:
    """Service for media entry operations."""

    def __init__(self, db_session: Session):
        self.db = db_session

    def get_channels(self) -> List[str]:
        """Get all distinct channels."""
        result = self.db.query(distinct(MediaEntry.channel)).order_by(MediaEntry.channel).all()
        return [r[0] for r in result]

    def get_themes(
        self,
        channel: Optional[str] = None,
        min_timestamp: int = 0,
        limit: int = DEFAULT_LIMIT,
        offset: int = DEFAULT_OFFSET
    ) -> Tuple[List[str], int]:
        """Get themes with optional filtering and pagination."""
        query = self.db.query(distinct(MediaEntry.theme))

        if channel:
            query = query.filter(MediaEntry.channel == channel)

        if min_timestamp > 0:
            query = query.filter(MediaEntry.timestamp >= min_timestamp)

        # Get total count
        total = query.count()

        # Apply pagination
        limit = min(limit, MAX_LIMIT)
        result = query.order_by(MediaEntry.theme).offset(offset).limit(limit).all()

        return [r[0] for r in result], total

    def get_titles(
        self,
        channel: Optional[str] = None,
        theme: Optional[str] = None,
        min_timestamp: int = 0,
        limit: int = DEFAULT_LIMIT,
        offset: int = DEFAULT_OFFSET
    ) -> Tuple[List[dict], int]:
        """Get titles (entries) with optional filtering and pagination."""
        query = self.db.query(MediaEntry)

        if channel:
            query = query.filter(MediaEntry.channel == channel)

        if theme:
            query = query.filter(MediaEntry.theme == theme)

        if min_timestamp > 0:
            query = query.filter(MediaEntry.timestamp >= min_timestamp)

        # Get total count
        total = query.count()

        # Apply pagination and ordering
        limit = min(limit, MAX_LIMIT)
        result = query.order_by(MediaEntry.timestamp.desc()).offset(offset).limit(limit).all()

        return [entry.to_dict() for entry in result], total

    def get_entry(
        self,
        channel: str,
        theme: str,
        title: str
    ) -> Optional[dict]:
        """Get a single entry by channel, theme, and title."""
        entry = self.db.query(MediaEntry).filter(
            MediaEntry.channel == channel,
            MediaEntry.theme == theme,
            MediaEntry.title == title
        ).first()

        return entry.to_dict() if entry else None

    def get_entry_by_theme(self, theme: str, title: str) -> Optional[dict]:
        """Get a single entry by theme and title."""
        entry = self.db.query(MediaEntry).filter(
            MediaEntry.theme == theme,
            MediaEntry.title == title
        ).first()

        return entry.to_dict() if entry else None

    def get_entry_by_title(self, title: str) -> Optional[dict]:
        """Get a single entry by title only."""
        entry = self.db.query(MediaEntry).filter(
            MediaEntry.title == title
        ).first()

        return entry.to_dict() if entry else None

    def get_recent_entries(
        self,
        min_timestamp: int,
        limit: int = DEFAULT_LIMIT
    ) -> List[dict]:
        """Get recent entries since timestamp."""
        limit = min(limit, MAX_LIMIT)
        entries = self.db.query(MediaEntry).filter(
            MediaEntry.timestamp >= min_timestamp
        ).order_by(MediaEntry.timestamp.desc()).limit(limit).all()

        return [entry.to_dict() for entry in entries]

    def get_diff_entries(
        self,
        since_timestamp: int,
        limit: int = MAX_LIMIT
    ) -> List[dict]:
        """Get entries newer than timestamp for incremental sync."""
        entries = self.db.query(MediaEntry).filter(
            MediaEntry.timestamp > since_timestamp
        ).order_by(MediaEntry.timestamp).limit(limit).all()

        return [entry.to_dict() for entry in entries]

    def get_count(self) -> int:
        """Get total entry count."""
        return self.db.query(func.count(MediaEntry.id)).scalar()

    def get_stats(self) -> dict:
        """Get database statistics."""
        total_count = self.get_count()
        channel_count = self.db.query(func.count(distinct(MediaEntry.channel))).scalar()
        theme_count = self.db.query(func.count(distinct(MediaEntry.theme))).scalar()

        # Get latest timestamp
        latest = self.db.query(func.max(MediaEntry.timestamp)).scalar() or 0

        # Get new entries count
        new_count = self.db.query(func.count(MediaEntry.id)).filter(
            MediaEntry.isNew == True
        ).scalar()

        return {
            "totalEntries": total_count,
            "channelCount": channel_count,
            "themeCount": theme_count,
            "latestTimestamp": latest,
            "newEntriesCount": new_count
        }

    def insert_entries(self, entries: List[dict]) -> int:
        """Bulk insert entries."""
        count = 0
        for entry_data in entries:
            entry = MediaEntry(**entry_data)
            self.db.merge(entry)  # Use merge for upsert behavior
            count += 1

        self.db.commit()
        return count

    def delete_all_entries(self) -> int:
        """Delete all entries."""
        count = self.db.query(MediaEntry).delete()
        self.db.commit()
        return count
