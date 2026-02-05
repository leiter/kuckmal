"""URL utility functions for handling pipe-delimited URL format."""


def reconstruct_url(base_url: str, relative_url: str) -> str:
    """
    Reconstruct a full URL from the pipe-delimited format used in Filmliste.

    Format: "offset|suffix" where offset is the number of characters to keep
    from the base URL and suffix is appended.

    Example:
        base_url = "https://example.com/video/stream.mp4"
        relative_url = "23|hd_stream.mp4"
        Result: "https://example.com/video/hd_stream.mp4"
    """
    if not relative_url or "|" not in relative_url:
        return relative_url

    try:
        parts = relative_url.split("|", 1)
        offset = int(parts[0])
        suffix = parts[1] if len(parts) > 1 else ""

        if offset > 0 and offset <= len(base_url):
            return base_url[:offset] + suffix
        return relative_url
    except (ValueError, IndexError):
        return relative_url


def clean_url(url: str) -> str:
    """Clean and normalize a URL."""
    if not url:
        return ""
    return url.strip()


def resolve_url(entry: dict) -> dict:
    """
    Resolve all URLs in a media entry, reconstructing relative URLs.

    Returns a copy of the entry with resolved URLs.
    """
    if not entry:
        return entry

    result = entry.copy()
    base_url = result.get("url", "")

    # Resolve smallUrl if it's in pipe-delimited format
    if result.get("smallUrl"):
        result["smallUrl"] = reconstruct_url(base_url, result["smallUrl"])

    # Resolve hdUrl if it's in pipe-delimited format
    if result.get("hdUrl"):
        result["hdUrl"] = reconstruct_url(base_url, result["hdUrl"])

    return result


def get_best_quality_url(entry: dict) -> str:
    """
    Get the best quality URL available for an entry.

    Priority: HD > Normal > Small
    """
    resolved = resolve_url(entry)

    if resolved.get("hdUrl"):
        return resolved["hdUrl"]
    if resolved.get("url"):
        return resolved["url"]
    if resolved.get("smallUrl"):
        return resolved["smallUrl"]

    return ""
