"""Film list download service with progress tracking."""

import requests
from typing import Callable, Optional
from threading import Lock

from config import FILMLIST_URL, DIFF_URL, DOWNLOAD_TIMEOUT, DOWNLOAD_CHUNK_SIZE


class DownloadService:
    """Service for downloading film list files with progress tracking."""

    def __init__(self):
        self._lock = Lock()
        self.is_downloading = False
        self.should_cancel = False
        self.status = {
            "state": "idle",
            "progress": 0,
            "downloaded": 0,
            "total": 0,
            "message": ""
        }

    def download_filmlist(
        self,
        output_path: str,
        progress_callback: Optional[Callable[[int, int, int], None]] = None
    ) -> bool:
        """Download the full film list."""
        return self._download_file(FILMLIST_URL, output_path, progress_callback)

    def download_diff(
        self,
        output_path: str,
        progress_callback: Optional[Callable[[int, int, int], None]] = None
    ) -> bool:
        """Download the diff file."""
        return self._download_file(DIFF_URL, output_path, progress_callback)

    def _download_file(
        self,
        url: str,
        output_path: str,
        progress_callback: Optional[Callable[[int, int, int], None]]
    ) -> bool:
        """Download a file with progress tracking."""
        with self._lock:
            if self.is_downloading:
                self.status = {
                    "state": "error",
                    "progress": 0,
                    "downloaded": 0,
                    "total": 0,
                    "message": "Download already in progress"
                }
                return False

            self.is_downloading = True
            self.should_cancel = False
            self.status = {
                "state": "downloading",
                "progress": 0,
                "downloaded": 0,
                "total": 0,
                "message": "Starting download..."
            }

        try:
            # Get file size first
            head = requests.head(url, timeout=DOWNLOAD_TIMEOUT)
            total_bytes = int(head.headers.get('content-length', 0))

            self.status["total"] = total_bytes
            self.status["message"] = f"Downloading {total_bytes / (1024*1024):.1f} MB"

            # Download with streaming
            response = requests.get(url, stream=True, timeout=DOWNLOAD_TIMEOUT)
            response.raise_for_status()

            downloaded = 0
            with open(output_path, 'wb') as f:
                for chunk in response.iter_content(chunk_size=DOWNLOAD_CHUNK_SIZE):
                    if self.should_cancel:
                        self.status = {
                            "state": "cancelled",
                            "progress": 0,
                            "downloaded": downloaded,
                            "total": total_bytes,
                            "message": "Download cancelled"
                        }
                        return False

                    if chunk:
                        f.write(chunk)
                        downloaded += len(chunk)

                        if total_bytes > 0:
                            progress = int((downloaded / total_bytes) * 100)
                            self.status["progress"] = progress
                            self.status["downloaded"] = downloaded

                            if progress_callback:
                                progress_callback(progress, downloaded, total_bytes)

            self.status = {
                "state": "download_complete",
                "progress": 100,
                "downloaded": downloaded,
                "total": total_bytes,
                "message": "Download complete"
            }
            return True

        except requests.Timeout:
            self.status = {
                "state": "error",
                "progress": 0,
                "downloaded": 0,
                "total": 0,
                "message": "Download timed out"
            }
            return False
        except requests.RequestException as e:
            self.status = {
                "state": "error",
                "progress": 0,
                "downloaded": 0,
                "total": 0,
                "message": f"Download error: {str(e)}"
            }
            return False
        except IOError as e:
            self.status = {
                "state": "error",
                "progress": 0,
                "downloaded": 0,
                "total": 0,
                "message": f"File write error: {str(e)}"
            }
            return False
        finally:
            with self._lock:
                self.is_downloading = False

    def cancel(self):
        """Cancel ongoing download."""
        self.should_cancel = True

    def get_status(self) -> dict:
        """Get current download status."""
        return self.status.copy()

    def check_for_updates(self) -> Optional[dict]:
        """Check if updates are available by examining file headers."""
        try:
            response = requests.head(FILMLIST_URL, timeout=DOWNLOAD_TIMEOUT)
            return {
                "content_length": int(response.headers.get('content-length', 0)),
                "last_modified": response.headers.get('last-modified', ''),
                "etag": response.headers.get('etag', '')
            }
        except requests.RequestException:
            return None
