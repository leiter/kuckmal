package cut.the.crap.shared.util

import platform.Foundation.NSLocale
import platform.Foundation.countryCode
import platform.Foundation.currentLocale

actual fun currentRegionCode(): String? =
    NSLocale.currentLocale.countryCode?.takeIf { it.isNotEmpty() }?.uppercase()
