package cut.the.crap.shared.di

import cut.the.crap.shared.repository.MediaRepository
import cut.the.crap.shared.repository.TvosMockMediaRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Koin module for tvOS platform
 * Provides mock repository implementation for demonstration
 */
val tvosModule: Module = module {
    single<MediaRepository> { TvosMockMediaRepository() }
}
