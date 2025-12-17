package cut.the.crap.shared.di

import cut.the.crap.shared.repository.MediaRepository
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Helper object to initialize Koin for tvOS and provide access to dependencies
 */
object KoinHelper : KoinComponent {

    /**
     * Initialize Koin with tvOS modules
     * Call this from tvOS app startup (e.g., in App init)
     */
    fun initKoin() {
        startKoin {
            modules(tvosModule)
        }
    }

    /**
     * Get the MediaRepository instance
     * Use this from Swift to access media data
     */
    fun getMediaRepository(): MediaRepository {
        val repository: MediaRepository by inject()
        return repository
    }
}

/**
 * Extension to expose KoinHelper to Swift/ObjC
 */
@Suppress("unused")
fun initKoin() {
    KoinHelper.initKoin()
}

@Suppress("unused")
fun getMediaRepository(): MediaRepository {
    return KoinHelper.getMediaRepository()
}
