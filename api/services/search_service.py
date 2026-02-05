"""Search service with caching and word-order independent matching."""

import time
from typing import List, Dict, Tuple, Optional
from sqlalchemy import or_
from sqlalchemy.orm import Session

from models import MediaEntry
from config import SEARCH_CACHE_TTL, SEARCH_CACHE_MAX_SIZE, DEFAULT_LIMIT, MAX_LIMIT


class SearchService:
    """Search service with caching and multi-word matching."""

    def __init__(self):
        self._cache: Dict[str, Tuple[List[dict], float]] = {}

    def search(
        self,
        db: Session,
        query: str,
        channel: Optional[str] = None,
        theme: Optional[str] = None,
        limit: int = DEFAULT_LIMIT,
        offset: int = 0
    ) -> Tuple[List[dict], int]:
        """
        Full-text search with word-order independent matching.
        All query words must be present in title, description, or theme.
        """
        cache_key = f"{query}|{channel}|{theme}|{limit}|{offset}"

        # Check cache
        if cache_key in self._cache:
            results, cached_time = self._cache[cache_key]
            if time.time() - cached_time < SEARCH_CACHE_TTL:
                return results, len(results)

        # Parse query into words
        words = query.lower().split()
        if not words:
            return [], 0

        # Build query
        db_query = db.query(MediaEntry)

        # Apply channel/theme filters
        if channel:
            db_query = db_query.filter(MediaEntry.channel == channel)
        if theme:
            db_query = db_query.filter(MediaEntry.theme == theme)

        # Apply word filters - each word must appear in title, description, or theme
        for word in words:
            word_pattern = f"%{word}%"
            db_query = db_query.filter(
                or_(
                    MediaEntry.title.ilike(word_pattern),
                    MediaEntry.description.ilike(word_pattern),
                    MediaEntry.theme.ilike(word_pattern)
                )
            )

        # Get total count before pagination
        total = db_query.count()

        # Apply ordering and pagination
        limit = min(limit, MAX_LIMIT)
        results = db_query.order_by(MediaEntry.timestamp.desc()).offset(offset).limit(limit).all()

        result_dicts = [entry.to_dict() for entry in results]

        # Cache results
        self._update_cache(cache_key, result_dicts)

        return result_dicts, total

    def _update_cache(self, key: str, results: List[dict]):
        """LRU-style cache update."""
        if len(self._cache) >= SEARCH_CACHE_MAX_SIZE:
            # Remove oldest entry
            oldest_key = min(
                self._cache.keys(),
                key=lambda k: self._cache[k][1]
            )
            del self._cache[oldest_key]
        self._cache[key] = (results, time.time())

    def clear_cache(self):
        """Clear the search cache."""
        self._cache.clear()
