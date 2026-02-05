"""Film list download and import endpoints with SSE progress."""

import os
import json
import tempfile
from flask import Blueprint, Response, jsonify, current_app

from database import get_db_session
from models import MediaEntry

filmlist_bp = Blueprint('filmlist', __name__)


@filmlist_bp.route('/api/filmlist/download', methods=['POST'])
def download_filmlist():
    """Download and import full film list with SSE progress."""

    def generate():
        download_service = current_app.config.get('download_service')
        parser_service = current_app.config.get('parser_service')

        with tempfile.TemporaryDirectory() as tmpdir:
            xz_path = os.path.join(tmpdir, "filmlist.xz")
            json_path = os.path.join(tmpdir, "filmlist.json")

            # Stage 1: Download
            yield f"data: {json.dumps({'stage': 'download', 'progress': 0, 'message': 'Starting download...'})}\n\n"

            def download_progress(pct, downloaded, total):
                pass  # Progress tracked in service status

            if not download_service.download_filmlist(xz_path, download_progress):
                status = download_service.get_status()
                yield f"data: {json.dumps({'stage': 'error', 'message': status.get('message', 'Download failed')})}\n\n"
                return

            yield f"data: {json.dumps({'stage': 'download', 'progress': 100, 'message': 'Download complete'})}\n\n"

            # Stage 2: Decompress
            yield f"data: {json.dumps({'stage': 'decompress', 'progress': 0, 'message': 'Decompressing...'})}\n\n"

            if not parser_service.decompress_xz(xz_path, json_path):
                status = parser_service.get_status()
                yield f"data: {json.dumps({'stage': 'error', 'message': status.get('message', 'Decompression failed')})}\n\n"
                return

            yield f"data: {json.dumps({'stage': 'decompress', 'progress': 100, 'message': 'Decompression complete'})}\n\n"

            # Stage 3: Import
            yield f"data: {json.dumps({'stage': 'import', 'progress': 0, 'message': 'Clearing existing data...'})}\n\n"

            db = get_db_session()
            try:
                # Clear existing entries
                db.query(MediaEntry).delete()
                db.commit()

                yield f"data: {json.dumps({'stage': 'import', 'progress': 5, 'message': 'Importing entries...'})}\n\n"

                def import_progress(count):
                    pass  # Progress tracked in service status

                total = parser_service.parse_and_import(json_path, db, import_progress, is_diff=False)

                yield f"data: {json.dumps({'stage': 'complete', 'progress': 100, 'imported': total, 'message': f'Import complete: {total} entries'})}\n\n"

            except Exception as e:
                db.rollback()
                yield f"data: {json.dumps({'stage': 'error', 'message': f'Import failed: {str(e)}'})}\n\n"
            finally:
                db.close()

    return Response(
        generate(),
        mimetype='text/event-stream',
        headers={
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
            'X-Accel-Buffering': 'no'
        }
    )


@filmlist_bp.route('/api/filmlist/diff', methods=['POST'])
def apply_diff():
    """Download and apply diff file with SSE progress."""

    def generate():
        download_service = current_app.config.get('download_service')
        parser_service = current_app.config.get('parser_service')

        with tempfile.TemporaryDirectory() as tmpdir:
            xz_path = os.path.join(tmpdir, "diff.xz")
            json_path = os.path.join(tmpdir, "diff.json")

            # Stage 1: Download diff
            yield f"data: {json.dumps({'stage': 'download', 'progress': 0, 'message': 'Downloading diff...'})}\n\n"

            if not download_service.download_diff(xz_path):
                status = download_service.get_status()
                yield f"data: {json.dumps({'stage': 'error', 'message': status.get('message', 'Download failed')})}\n\n"
                return

            yield f"data: {json.dumps({'stage': 'download', 'progress': 100, 'message': 'Download complete'})}\n\n"

            # Stage 2: Decompress
            yield f"data: {json.dumps({'stage': 'decompress', 'progress': 0, 'message': 'Decompressing...'})}\n\n"

            if not parser_service.decompress_xz(xz_path, json_path):
                status = parser_service.get_status()
                yield f"data: {json.dumps({'stage': 'error', 'message': status.get('message', 'Decompression failed')})}\n\n"
                return

            yield f"data: {json.dumps({'stage': 'decompress', 'progress': 100, 'message': 'Decompression complete'})}\n\n"

            # Stage 3: Apply diff
            yield f"data: {json.dumps({'stage': 'import', 'progress': 0, 'message': 'Applying diff...'})}\n\n"

            db = get_db_session()
            try:
                total = parser_service.parse_and_import(json_path, db, None, is_diff=True)
                yield f"data: {json.dumps({'stage': 'complete', 'progress': 100, 'imported': total, 'message': f'Diff applied: {total} entries updated'})}\n\n"
            except Exception as e:
                db.rollback()
                yield f"data: {json.dumps({'stage': 'error', 'message': f'Diff apply failed: {str(e)}'})}\n\n"
            finally:
                db.close()

    return Response(
        generate(),
        mimetype='text/event-stream',
        headers={
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive',
            'X-Accel-Buffering': 'no'
        }
    )


@filmlist_bp.route('/api/filmlist/status', methods=['GET'])
def get_status():
    """Get current download/import status."""
    download_service = current_app.config.get('download_service')
    parser_service = current_app.config.get('parser_service')

    download_status = download_service.get_status()
    parser_status = parser_service.get_status()

    # Return the most relevant status
    if download_status.get('state') == 'downloading':
        return jsonify(download_status)
    elif parser_status.get('state') in ['decompressing', 'parsing']:
        return jsonify(parser_status)
    else:
        return jsonify({
            "download": download_status,
            "parser": parser_status
        })


@filmlist_bp.route('/api/filmlist/cancel', methods=['POST'])
def cancel():
    """Cancel ongoing download/import operation."""
    download_service = current_app.config.get('download_service')
    parser_service = current_app.config.get('parser_service')

    download_service.cancel()
    parser_service.cancel()

    return jsonify({
        "cancelled": True,
        "message": "Cancellation requested"
    })
