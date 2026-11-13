package io.github.aedev.flow.youtubesync.net

import io.github.aedev.flow.network.AppProxyManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier

/**
 * The HTTP client of the official channel (ADR 0001). Separate from the clients of playback and
 * browsing: no cookies, no cache, no InnerTube or NewPipe headers, so the Linked Account is never
 * tied to them. It still follows the app's proxy setting.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class YouTubeSyncHttp

object YouTubeSyncHttpClient {
    /** The client's settings, before the proxy is applied: kept apart so a test can check them. */
    fun builder(): OkHttpClient.Builder =
        OkHttpClient
            .Builder()
            .cache(null)
            .cookieJar(okhttp3.CookieJar.NO_COOKIES)
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

    fun create(): OkHttpClient = AppProxyManager.buildLive(builder())
}
