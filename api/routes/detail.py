"""Detail endpoints for single entry retrieval."""

from flask import Blueprint, request, jsonify

from database import get_db_session
from services.media_service import MediaService
from utils.url_utils import resolve_url

detail_bp = Blueprint('detail', __name__)


@detail_bp.route('/api/entry', methods=['GET'])
def get_entry():
    """Get single entry by channel, theme, and title (all required)."""
    channel = request.args.get('channel')
    theme = request.args.get('theme')
    title = request.args.get('title')

    if not all([channel, theme, title]):
        return jsonify({
            "error": "Missing required parameters: channel, theme, title",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        service = MediaService(db)
        entry = service.get_entry(channel, theme, title)

        if entry:
            return jsonify({
                "data": resolve_url(entry)
            })
        else:
            return jsonify({
                "error": "Entry not found",
                "code": 404
            }), 404
    finally:
        db.close()


@detail_bp.route('/api/entry/by-theme', methods=['GET'])
def get_entry_by_theme():
    """Get entry by theme and title."""
    theme = request.args.get('theme')
    title = request.args.get('title')

    if not all([theme, title]):
        return jsonify({
            "error": "Missing required parameters: theme, title",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        service = MediaService(db)
        entry = service.get_entry_by_theme(theme, title)

        if entry:
            return jsonify({
                "data": resolve_url(entry)
            })
        else:
            return jsonify({
                "error": "Entry not found",
                "code": 404
            }), 404
    finally:
        db.close()


@detail_bp.route('/api/entry/by-title', methods=['GET'])
def get_entry_by_title():
    """Get entry by title only."""
    title = request.args.get('title')

    if not title:
        return jsonify({
            "error": "Missing required parameter: title",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        service = MediaService(db)
        entry = service.get_entry_by_title(title)

        if entry:
            return jsonify({
                "data": resolve_url(entry)
            })
        else:
            return jsonify({
                "error": "Entry not found",
                "code": 404
            }), 404
    finally:
        db.close()
