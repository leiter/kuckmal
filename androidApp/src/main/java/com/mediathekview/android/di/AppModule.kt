package com.mediathekview.android.di

import com.mediathekview.android.compose.models.ComposeViewModel
import com.mediathekview.android.data.MediaListParser
import com.mediathekview.android.data.MediaViewModel
import com.mediathekview.android.repository.DownloadRepository
import com.mediathekview.android.repository.MediaRepository
import com.mediathekview.android.repository.MediaRepositoryImpl
import com.mediathekview.android.service.DownloadService
import com.mediathekview.android.util.UpdateChecker
import com.mediathekview.shared.database.AppDatabase
import com.mediathekview.shared.database.getDatabaseBuilder
import com.mediathekview.shared.database.getRoomDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module
 * Defines how to create and provide dependencies throughout the app
 */
val appModule = module {

    // Database - Singleton (using shared KMP Room)
    single {
        getRoomDatabase(
            getDatabaseBuilder(androidApplication())
        )
    }

    // DAO - Singleton (provided by database)
    single { get<AppDatabase>().mediaDao() }

    // Parser - Factory (create new instance each time)
    factory { MediaListParser() }

    // MediaRepository - Singleton
    single<MediaRepository> {
        MediaRepositoryImpl(
            mediaDao = get(),
            parser = get()
        )
    }

    // UpdateChecker - Singleton
    single {
        UpdateChecker(androidContext())
    }

    // DownloadService - Singleton (refactored with no Activity/ViewModel deps)
    single {
        DownloadService(
            context = androidContext(),
            updateChecker = get()
        )
    }

    // DownloadRepository - Singleton
    single {
        DownloadRepository(
            context = androidContext(),
            downloadService = get()
        )
    }

    // MediaViewModel - scoped to Activity/Fragment lifecycle
    viewModel {
        MediaViewModel(
            application = androidApplication(),
            repository = get(),
            downloadRepository = get(),
            updateChecker = get()
        )
    }

    // ComposeViewModel - scoped to Activity/Fragment lifecycle
    // Note: Uses standard ViewModel (not AndroidViewModel)
    // Call viewModel.initializeContext(context) after obtaining the ViewModel
    viewModel {
        ComposeViewModel(
            repository = get(),
            downloadRepository = get(),
            updateChecker = get()
        )
    }
}
