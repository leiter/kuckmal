"""Data sync endpoints for incremental updates and bulk operations."""

from flask import Blueprint, request, jsonify

from database import get_db_session
from services.media_service import MediaService
from config import DEFAULT_LIMIT, MAX_LIMIT

sync_bp = Blueprint('sync', __name__)


@sync_bp.route('/api/entries/recent', methods=['GET'])
def get_recent_entries():
    """Get recent entries since timestamp."""
    min_timestamp = request.args.get('minTimestamp', 0, type=int)
    limit = request.args.get('limit', DEFAULT_LIMIT, type=int)

    db = get_db_session()
    try:
        service = MediaService(db)
        entries = service.get_recent_entries(min_timestamp, limit)

        return jsonify({
            "data": entries,
            "count": len(entries),
            "minTimestamp": min_timestamp
        })
    finally:
        db.close()


@sync_bp.route('/api/entries/count', methods=['GET'])
def get_entries_count():
    """Get total entry count."""
    db = get_db_session()
    try:
        service = MediaService(db)
        count = service.get_count()

        return jsonify({
            "count": count
        })
    finally:
        db.close()


@sync_bp.route('/api/entries/diff', methods=['GET'])
def get_diff_entries():
    """Get entries newer than timestamp for incremental sync."""
    since = request.args.get('since', 0, type=int)
    limit = request.args.get('limit', MAX_LIMIT, type=int)

    if since <= 0:
        return jsonify({
            "error": "Missing required parameter: since (timestamp)",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        service = MediaService(db)
        entries = service.get_diff_entries(since, limit)

        return jsonify({
            "data": entries,
            "count": len(entries),
            "since": since
        })
    finally:
        db.close()


@sync_bp.route('/api/entries', methods=['POST'])
def bulk_insert_entries():
    """Bulk insert entries."""
    data = request.get_json()

    if not data or not isinstance(data, list):
        return jsonify({
            "error": "Request body must be a JSON array of entries",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        service = MediaService(db)
        count = service.insert_entries(data)

        return jsonify({
            "inserted": count,
            "message": f"Successfully inserted {count} entries"
        })
    except Exception as e:
        db.rollback()
        return jsonify({
            "error": f"Insert failed: {str(e)}",
            "code": 500
        }), 500
    finally:
        db.close()


@sync_bp.route('/api/entries', methods=['DELETE'])
def delete_all_entries():
    """Clear all entries."""
    db = get_db_session()
    try:
        service = MediaService(db)
        count = service.delete_all_entries()

        return jsonify({
            "deleted": count,
            "message": f"Successfully deleted {count} entries"
        })
    except Exception as e:
        db.rollback()
        return jsonify({
            "error": f"Delete failed: {str(e)}",
            "code": 500
        }), 500
    finally:
        db.close()
