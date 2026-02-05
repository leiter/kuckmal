"""Configuration settings for the Flask API."""

import os

# Base directory
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

# Database
DATABASE_PATH = os.path.join(BASE_DIR, "data", "kuckmal.db")

# Film list URLs
FILMLIST_URL = "https://verteiler1.mediathekview.de/Filmliste-akt.xz"
DIFF_URL = "https://verteiler1.mediathekview.de/Filmliste-diff.xz"

# Download settings
DOWNLOAD_TIMEOUT = 30  # seconds
DOWNLOAD_CHUNK_SIZE = 8192  # bytes

# Parser settings
PARSER_BATCH_SIZE = 5000  # entries per batch insert

# Search settings
SEARCH_CACHE_TTL = 300  # 5 minutes
SEARCH_CACHE_MAX_SIZE = 50

# Default pagination
DEFAULT_LIMIT = 100
DEFAULT_OFFSET = 0
MAX_LIMIT = 10000
