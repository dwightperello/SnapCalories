package com.example.snapcalories

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * The app's Application class — created once, before any Activity, and lives as long as the app.
 *
 * `@HiltAndroidApp` is the starting point of Hilt (dependency injection). It makes Hilt generate the
 * app-wide "container" that holds every `@Singleton` (e.g. our `Clock`). Without it, no
 * `@AndroidEntryPoint` / `@HiltViewModel` / `@Inject` works.
 *
 * Remember: register it in AndroidManifest.xml → `<application android:name=".SnapCaloriesApp" ...>`
 */
@HiltAndroidApp
class SnapCaloriesApp : Application()
