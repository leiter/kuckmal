package com.mediathekview.android.compose.screens

// This file previously contained ComposeMediaState wrapper class.
// State management has been consolidated into ComposeViewModel directly.
// See compose/models/ComposeViewModel.kt for all state management.
//
// The wrapper class was removed as part of the architecture cleanup to:
// - Follow single source of truth pattern
// - Simplify state flow (ViewModel -> Compose UI directly)
// - Improve testability
// - Reduce memory overhead
