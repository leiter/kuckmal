package cut.the.crap.shared.di

import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.repository.TvosApiMediaRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for tvOS platform
 * Provides API repository - shows error when offline instead of mock data
 */
val tvosModule: Module = module {
    single<MediaRepository> { TvosApiMediaRepository() }
}
