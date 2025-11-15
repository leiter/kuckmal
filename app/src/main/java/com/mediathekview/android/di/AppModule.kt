package com.mediathekview.android.di

import androidx.room.Room
import com.mediathekview.android.compose.models.ComposeViewModel
import com.mediathekview.android.data.MediaListParser
import com.mediathekview.android.data.MediaViewModel
import com.mediathekview.android.database.AppDatabase
import com.mediathekview.android.repository.DownloadRepository
import com.mediathekview.android.repository.MediaRepository
import com.mediathekview.android.repository.MediaRepositoryImpl
import com.mediathekview.android.service.DownloadService
import com.mediathekview.android.util.UpdateChecker
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module
 * Defines how to create and provide dependencies throughout the app
 */
val appModule = module {

    // Database - Singleton
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "mediathekview.db"
        )
            .fallbackToDestructiveMigration() // For development
            .build()
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

    // MediaViewModel - scoped to Activity/Fragment lifecycle
    viewModel {
        ComposeViewModel(
            application = androidApplication(),
            repository = get(),
            downloadRepository = get(),
            updateChecker = get()
        )
    }
}
