"""Browse endpoints for channels, themes, and titles."""

from flask import Blueprint, request, jsonify

from database import get_db_session
from services.media_service import MediaService
from config import DEFAULT_LIMIT, DEFAULT_OFFSET

browse_bp = Blueprint('browse', __name__)


@browse_bp.route('/api/channels', methods=['GET'])
def get_channels():
    """Get all distinct channels."""
    db = get_db_session()
    try:
        service = MediaService(db)
        channels = service.get_channels()
        return jsonify({
            "data": channels,
            "count": len(channels)
        })
    finally:
        db.close()


@browse_bp.route('/api/themes', methods=['GET'])
def get_themes():
    """Get themes with optional filtering and pagination."""
    channel = request.args.get('channel')
    min_timestamp = request.args.get('minTimestamp', 0, type=int)
    limit = request.args.get('limit', DEFAULT_LIMIT, type=int)
    offset = request.args.get('offset', DEFAULT_OFFSET, type=int)

    db = get_db_session()
    try:
        service = MediaService(db)
        themes, total = service.get_themes(
            channel=channel,
            min_timestamp=min_timestamp,
            limit=limit,
            offset=offset
        )
        return jsonify({
            "data": themes,
            "count": len(themes),
            "total": total,
            "offset": offset,
            "limit": limit
        })
    finally:
        db.close()


@browse_bp.route('/api/titles', methods=['GET'])
def get_titles():
    """Get titles for a channel/theme with optional filtering and pagination."""
    channel = request.args.get('channel')
    theme = request.args.get('theme')
    min_timestamp = request.args.get('minTimestamp', 0, type=int)
    limit = request.args.get('limit', DEFAULT_LIMIT, type=int)
    offset = request.args.get('offset', DEFAULT_OFFSET, type=int)

    db = get_db_session()
    try:
        service = MediaService(db)
        titles, total = service.get_titles(
            channel=channel,
            theme=theme,
            min_timestamp=min_timestamp,
            limit=limit,
            offset=offset
        )
        return jsonify({
            "data": titles,
            "count": len(titles),
            "total": total,
            "offset": offset,
            "limit": limit
        })
    finally:
        db.close()


@browse_bp.route('/api/broadcasters', methods=['GET'])
def get_broadcasters():
    """Get broadcaster info with brand colors."""
    broadcasters = [
        {"name": "ARD", "brandColor": 0x003366, "abbreviation": "ARD"},
        {"name": "ZDF", "brandColor": 0xFA7D19, "abbreviation": "ZDF"},
        {"name": "3Sat", "brandColor": 0x333333, "abbreviation": "3sat"},
        {"name": "ARTE.DE", "brandColor": 0xF05A23, "abbreviation": "ARTE"},
        {"name": "ARTE.FR", "brandColor": 0xF05A23, "abbreviation": "ARTE"},
        {"name": "BR", "brandColor": 0x0066B3, "abbreviation": "BR"},
        {"name": "HR", "brandColor": 0x004B93, "abbreviation": "HR"},
        {"name": "KiKA", "brandColor": 0x85C441, "abbreviation": "KiKA"},
        {"name": "MDR", "brandColor": 0x00519E, "abbreviation": "MDR"},
        {"name": "NDR", "brandColor": 0x003B7E, "abbreviation": "NDR"},
        {"name": "ORF", "brandColor": 0x8C8C8C, "abbreviation": "ORF"},
        {"name": "PHOENIX", "brandColor": 0xFF6600, "abbreviation": "PHX"},
        {"name": "RBB", "brandColor": 0xAD1D24, "abbreviation": "RBB"},
        {"name": "SR", "brandColor": 0x009CDC, "abbreviation": "SR"},
        {"name": "SRF", "brandColor": 0xC8002D, "abbreviation": "SRF"},
        {"name": "SWR", "brandColor": 0xFF6600, "abbreviation": "SWR"},
        {"name": "WDR", "brandColor": 0x00355F, "abbreviation": "WDR"},
        {"name": "ZDF-tivi", "brandColor": 0x00AAE1, "abbreviation": "tivi"},
        {"name": "DW", "brandColor": 0x003366, "abbreviation": "DW"},
        {"name": "FUNK.net", "brandColor": 0xE10078, "abbreviation": "FUNK"},
        {"name": "Radio Bremen TV", "brandColor": 0x007BC4, "abbreviation": "RB"},
    ]
    return jsonify({
        "data": broadcasters,
        "count": len(broadcasters)
    })
