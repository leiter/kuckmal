"""Flask application factory and configuration."""

from flask import Flask, jsonify

from database import init_db, get_db_session
from services.search_service import SearchService
from services.download_service import DownloadService
from services.parser_service import ParserService
from routes.browse import browse_bp
from routes.detail import detail_bp
from routes.search import search_bp
from routes.sync import sync_bp
from routes.filmlist import filmlist_bp


def create_app(load_sample: bool = False):
    """Create and configure the Flask application."""
    app = Flask(__name__)

    # Enable CORS for all routes (required for webOS/web app)
    @app.after_request
    def add_cors_headers(response):
        response.headers['Access-Control-Allow-Origin'] = '*'
        response.headers['Access-Control-Allow-Methods'] = 'GET, POST, DELETE, OPTIONS'
        response.headers['Access-Control-Allow-Headers'] = 'Content-Type'
        return response

    # Handle OPTIONS preflight requests
    @app.route('/<path:path>', methods=['OPTIONS'])
    def options_handler(path):
        return '', 204

    # Initialize database
    init_db()

    # Initialize services
    app.config['search_service'] = SearchService()
    app.config['download_service'] = DownloadService()
    app.config['parser_service'] = ParserService()

    # Register blueprints
    app.register_blueprint(browse_bp)
    app.register_blueprint(detail_bp)
    app.register_blueprint(search_bp)
    app.register_blueprint(sync_bp)
    app.register_blueprint(filmlist_bp)

    # Health check endpoint
    @app.route('/api/health', methods=['GET'])
    def health():
        return jsonify({
            "status": "healthy",
            "service": "kuckmal-api"
        })

    # Stats endpoint
    @app.route('/api/stats', methods=['GET'])
    def stats():
        from services.media_service import MediaService
        db = get_db_session()
        try:
            service = MediaService(db)
            stats_data = service.get_stats()
            return jsonify(stats_data)
        finally:
            db.close()

    # Error handlers
    @app.errorhandler(400)
    def bad_request(error):
        return jsonify({
            "error": "Bad request",
            "code": 400
        }), 400

    @app.errorhandler(404)
    def not_found(error):
        return jsonify({
            "error": "Resource not found",
            "code": 404
        }), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({
            "error": "Internal server error",
            "code": 500
        }), 500

    # Load sample data if requested
    if load_sample:
        with app.app_context():
            from services.media_service import MediaService
            db = get_db_session()
            try:
                service = MediaService(db)
                if service.get_count() == 0:
                    print("Loading sample data...")
                    from data.sample_data import load_sample_data
                    count = load_sample_data(db, count=500)
                    print(f"Loaded {count} sample entries")
            finally:
                db.close()

    return app
