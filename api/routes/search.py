"""Search endpoints."""

from flask import Blueprint, request, jsonify, current_app

from database import get_db_session
from config import DEFAULT_LIMIT, DEFAULT_OFFSET

search_bp = Blueprint('search', __name__)


@search_bp.route('/api/search', methods=['GET'])
def search():
    """Full-text search with word-order independent matching."""
    query = request.args.get('q', '')
    channel = request.args.get('channel')
    theme = request.args.get('theme')
    limit = request.args.get('limit', DEFAULT_LIMIT, type=int)
    offset = request.args.get('offset', DEFAULT_OFFSET, type=int)

    if not query:
        return jsonify({
            "error": "Missing required parameter: q (search query)",
            "code": 400
        }), 400

    db = get_db_session()
    try:
        search_service = current_app.config.get('search_service')
        results, total = search_service.search(
            db=db,
            query=query,
            channel=channel,
            theme=theme,
            limit=limit,
            offset=offset
        )

        return jsonify({
            "data": results,
            "count": len(results),
            "total": total,
            "offset": offset,
            "limit": limit,
            "query": query
        })
    finally:
        db.close()
