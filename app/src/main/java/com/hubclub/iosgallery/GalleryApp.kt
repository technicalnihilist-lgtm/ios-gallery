package com.hubclub.iosgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import java.io.File

/**
 * iOS Gallery is a single-process, offline application.
 * The only global setup is a thumbnail loader with a small memory cache and a
 * local disk cache so scrolling stays smooth on low-end devices.
 */
class GalleryApp : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val cacheDir = File(this.cacheDir, "thumbnails")
        return ImageLoader.Builder(this)
            .components { add(VideoFrameDecoder.Factory()) }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(200L * 1024L * 1024L)
                    .build()
            }
            .crossfade(140)
            .allowHardware(true)
            .build()
    }
}
