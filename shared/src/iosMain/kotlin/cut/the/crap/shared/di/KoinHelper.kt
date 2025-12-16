package cut.the.crap.shared.di

import cut.the.crap.shared.viewmodel.SharedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.context.startKoin

/**
 * Helper object for iOS Koin initialization
 * Called from Swift: KoinHelperKt.doInitKoin()
 */
fun doInitKoin() {
    startKoin {
        modules(iosModule)
    }
}

/**
 * Helper class to access Koin dependencies from iOS
 * Usage in Kotlin: KoinHelper().getSharedViewModel()
 */
class KoinHelper : KoinComponent {
    fun getSharedViewModel(): SharedViewModel = get()
}
