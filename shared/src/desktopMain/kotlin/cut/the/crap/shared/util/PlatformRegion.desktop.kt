package cut.the.crap.shared.util

import java.util.Locale

actual fun currentRegionCode(): String? =
    Locale.getDefault().country.takeIf { it.isNotEmpty() }?.uppercase(Locale.ROOT)
