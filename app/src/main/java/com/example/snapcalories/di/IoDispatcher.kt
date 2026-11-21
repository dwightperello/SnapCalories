package com.example.snapcalories.di

import javax.inject.Qualifier

/**
 * A Hilt "qualifier" = a name tag for a dependency.
 *
 * Problem it solves: there are several `CoroutineDispatcher`s (IO, Default, Main). If a class just asks
 * for `CoroutineDispatcher`, Hilt can't know which one. With the tag, the class asks for
 * `@IoDispatcher dispatcher: CoroutineDispatcher` and Hilt picks the one provided with the same tag.
 *
 * Why inject a dispatcher at all instead of writing `Dispatchers.IO` directly?
 * → In tests we can pass a test dispatcher, so the code runs instantly and predictably.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
