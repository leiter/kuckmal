package cut.the.crap.shared.util

/**
 * The user's region as an ISO 3166-1 alpha-2 country code (e.g. "DE", "AT", "CH"),
 * read from the device's own locale settings.
 *
 * Used to decide whether to warn about geo-restricted media. Returns null when the
 * platform reports no region, in which case callers skip the warning.
 *
 * This deliberately performs no network request: the previous implementation asked a
 * third-party IP geolocation service, which leaked the user's IP address and was
 * blocked outright by App Transport Security on iOS.
 */
expect fun currentRegionCode(): String?
