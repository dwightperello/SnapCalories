package com.example.snapcalories.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import java.time.Clock
import javax.inject.Singleton

/**
 * A Hilt module = a "recipe book" telling Hilt how to create things it can't create by itself
 * (classes we don't own, like `Clock` or `Dispatchers.IO`, have no `@Inject constructor`).
 *
 * - `@Module`: this object contains recipes.
 * - `@InstallIn(SingletonComponent::class)`: the recipes are available app-wide, for the app's whole life.
 * - `object`: no state needed, so a Kotlin singleton object is enough.
 */
@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    // `@Provides` = "when someone asks for this type, call this function".
    // `@IoDispatcher` = only used when they ask for the dispatcher WITH that tag (see IoDispatcher.kt).
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    // Every "now" / "today" in the app comes from this Clock, never from LocalDate.now() directly.
    // Why: tests can inject a fixed clock (e.g. 23:59 in Manila) to test date edge cases.
    // `@Singleton` = create once, reuse the same instance everywhere.
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()
}
