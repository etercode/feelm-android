package org.feelm.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import okio.Path.Companion.toOkioPath
import org.feelm.app.di.AppContainer

/**
 * Holds the one [AppContainer] the app is wired from.
 *
 * Manual DI rather than Hilt: this app has one container, a handful of
 * singletons and no build-time codegen, so a Hilt setup would add a KSP round
 * to every build to save a dozen lines here.
 */
class FeelmApplication : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }

    /**
     * Posters, cached to disk.
     *
     * A browsing app is mostly a wall of the same few hundred images, and the
     * default memory-only cache re-downloads every one of them on relaunch.
     * 256 MB is roughly a thousand w500 posters — enough that scrolling back
     * up a rail, or opening the app on a train, costs nothing.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory()) }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("feelm_images").toOkioPath())
                    .maxSizeBytes(256L * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
}
