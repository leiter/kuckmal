#!/usr/bin/env python3
"""Entry point for running the Flask API server."""

import argparse
from app import create_app


def main():
    parser = argparse.ArgumentParser(description='Kuckmal Media API Server')
    parser.add_argument(
        '--host',
        default='0.0.0.0',
        help='Host to bind to (default: 0.0.0.0)'
    )
    parser.add_argument(
        '--port',
        type=int,
        default=5000,
        help='Port to bind to (default: 5000)'
    )
    parser.add_argument(
        '--debug',
        action='store_true',
        help='Run in debug mode'
    )
    parser.add_argument(
        '--sample-data',
        action='store_true',
        help='Load sample data on startup if database is empty'
    )

    args = parser.parse_args()

    app = create_app(load_sample=args.sample_data)

    print(f"Starting Kuckmal API server on {args.host}:{args.port}")
    print("Endpoints:")
    print("  GET  /api/health           - Health check")
    print("  GET  /api/stats            - Database statistics")
    print("  GET  /api/channels         - List all channels")
    print("  GET  /api/themes           - List themes (optional: channel, minTimestamp)")
    print("  GET  /api/titles           - List titles (optional: channel, theme)")
    print("  GET  /api/entry            - Get entry (required: channel, theme, title)")
    print("  GET  /api/entry/by-theme   - Get entry (required: theme, title)")
    print("  GET  /api/entry/by-title   - Get entry (required: title)")
    print("  GET  /api/search           - Search (required: q)")
    print("  GET  /api/entries/recent   - Recent entries (optional: minTimestamp)")
    print("  GET  /api/entries/count    - Total entry count")
    print("  GET  /api/entries/diff     - Diff entries (required: since)")
    print("  POST /api/entries          - Bulk insert entries")
    print("  DELETE /api/entries        - Delete all entries")
    print("  POST /api/filmlist/download - Download & import full film list")
    print("  POST /api/filmlist/diff    - Download & apply diff")
    print("  GET  /api/filmlist/status  - Current download/import status")
    print("  POST /api/filmlist/cancel  - Cancel download/import")
    print("  GET  /api/broadcasters     - Get broadcaster info")

    app.run(
        host=args.host,
        port=args.port,
        debug=args.debug,
        threaded=True
    )


if __name__ == '__main__':
    main()
