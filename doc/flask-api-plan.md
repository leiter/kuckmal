# Plan: Flask API Service for Kuckmal Media Database

## Overview
Create a Flask route module (Blueprint) to serve media data as a substitute for the local SQLite database. The service will be hosted at `kuckmal.cutthecrap.link/`.

## MediaEntry Data Model

```python
{
    "id": int,
    "channel": str,        # Broadcaster name (e.g., "ZDF", "ARD")
    "theme": str,          # Category/Show name
    "title": str,          # Episode title
    "date": str,           # Formatted date "YYYY-MM-DD"
    "time": str,           # Broadcast time
    "duration": str,       # Duration string
    "sizeMB": str,         # File size
    "description": str,    # Synopsis
    "url": str,            # Main video URL
    "website": str,        # Reference URL
    "subtitleUrl": str,    # Subtitle URL
    "smallUrl": str,       # Low-quality URL
    "hdUrl": str,          # HD video URL
    "timestamp": int,      # Unix timestamp (for sorting/filtering)
    "geo": str,            # Geographic restriction
    "isNew": bool          # New content marker
}
```

## API Endpoints

### Read Endpoints (GET)

| Endpoint | Parameters | Description |
|----------|------------|-------------|
| `/api/channels` | - | Get all unique channels |
| `/api/themes` | `channel`, `limit`, `offset`, `minTimestamp` | Get themes (optionally filtered by channel) |
| `/api/titles` | `channel`, `theme`, `minTimestamp` | Get titles for theme |
| `/api/entry` | `channel`, `theme`, `title` | Get single media entry |
| `/api/entries` | `channel`, `theme`, `limit`, `offset`, `minTimestamp` | Get entries with filters |
| `/api/recent` | `minTimestamp`, `limit` | Get recent entries |
| `/api/search` | `query`, `channel`, `theme`, `limit`, `offset` | Full-text search |
| `/api/count` | - | Get total entry count |

### Write Endpoints (POST/DELETE)

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/entries` | POST | Bulk insert entries |
| `/api/entries` | DELETE | Clear all entries |

## File Structure

```
script/
└── kuckmal_api/
    ├── __init__.py           # Flask app factory
    ├── routes.py             # Blueprint with all routes
    ├── models.py             # MongoDB document schema
    ├── database.py           # MongoDB connection
    └── requirements.txt      # Dependencies
```

## Files to Create

### 1. `script/kuckmal_api/__init__.py`
- Flask app factory
- Register Blueprint with prefix `/api`
- Configure CORS for mobile app access
- Initialize MongoDB connection

### 2. `script/kuckmal_api/models.py`
- MediaEntry document schema (using pymongo or mongoengine)
- Indexes: channel, theme, timestamp, text index on (title, description, theme)
- Unique compound index on (channel, theme, title)

### 3. `script/kuckmal_api/database.py`
- MongoDB connection via pymongo
- Collection: `media_entries`
- Database: `kuckmal`

### 4. `script/kuckmal_api/routes.py`
- Blueprint `kuckmal_bp`
- All query endpoints with proper pagination
- Text search using MongoDB `$text` operator
- Regex fallback for partial matches
- JSON responses matching Kotlin model field names

### 5. `script/kuckmal_api/requirements.txt`
```
flask>=3.0.0
flask-cors>=4.0.0
pymongo>=4.6.0
gunicorn>=21.0.0
```

## Implementation Details

### Search Logic (matching Android implementation)
- Search fields: title, description, theme
- Case-insensitive LIKE matching
- Order by timestamp DESC (newest first)
- Support multi-word queries (all words must match)

### Pagination
- Default limit: 100
- All list endpoints support `limit` and `offset`
- Themes endpoint: default limit 1200

### Timestamp Filtering
- `minTimestamp` parameter (Unix timestamp)
- 0 = no filter (return all)
- Used to filter old content

## MongoDB Indexes

```javascript
db.media_entries.createIndex({ "channel": 1 })
db.media_entries.createIndex({ "theme": 1 })
db.media_entries.createIndex({ "timestamp": -1 })
db.media_entries.createIndex({ "channel": 1, "theme": 1, "title": 1 }, { unique: true })
db.media_entries.createIndex({ "title": "text", "description": "text", "theme": "text" })
```

## Deployment Notes

- Host: `kuckmal.cutthecrap.link`
- WSGI server: Gunicorn recommended
- Database: MongoDB (local or Atlas)
- CORS: Enabled for all origins (public API)
- Auth: None required (public read-only)

## Example Queries

```bash
# Get all channels
curl https://kuckmal.cutthecrap.link/api/channels

# Get themes for ZDF
curl "https://kuckmal.cutthecrap.link/api/themes?channel=ZDF&limit=100"

# Search
curl "https://kuckmal.cutthecrap.link/api/search?query=Tatort&limit=50"

# Bulk insert entries (external data load)
curl -X POST https://kuckmal.cutthecrap.link/api/entries \
  -H "Content-Type: application/json" \
  -d '[{"channel":"ZDF","theme":"Tatort",...}]'
```
