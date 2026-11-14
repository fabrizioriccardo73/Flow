package io.github.aedev.flow.youtubesync.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.aedev.flow.data.local.safePreferencesDataStore
import io.github.aedev.flow.data.profile.StoreScope
import io.github.aedev.flow.youtubesync.account.AppAuthGateway
import io.github.aedev.flow.youtubesync.account.GoogleYouTubeIdentityApi
import io.github.aedev.flow.youtubesync.account.KeystoreSecretSealer
import io.github.aedev.flow.youtubesync.account.LinkedAccountStore
import io.github.aedev.flow.youtubesync.account.OAuthConfig
import io.github.aedev.flow.youtubesync.account.ThisDevice
import io.github.aedev.flow.youtubesync.account.OAuthGateway
import io.github.aedev.flow.youtubesync.account.SecretSealer
import io.github.aedev.flow.youtubesync.account.YouTubeIdentityApi
import io.github.aedev.flow.youtubesync.engine.SyncBaselineStore
import io.github.aedev.flow.youtubesync.local.FlowLocalPlaylists
import io.github.aedev.flow.youtubesync.local.FlowLocalSubscriptions
import io.github.aedev.flow.youtubesync.local.LocalPlaylists
import io.github.aedev.flow.youtubesync.local.LocalSubscriptions
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttp
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttpClient
import io.github.aedev.flow.youtubesync.remote.AccessTokenSource
import io.github.aedev.flow.youtubesync.remote.LinkedAccountTokenSource
import io.github.aedev.flow.youtubesync.remote.YouTubeAccount
import io.github.aedev.flow.youtubesync.remote.YouTubeDataApiAccount
import io.github.aedev.flow.youtubesync.runtime.Clock
import io.github.aedev.flow.youtubesync.runtime.SyncNotifications
import io.github.aedev.flow.youtubesync.runtime.SyncScheduling
import io.github.aedev.flow.youtubesync.runtime.SyncStatusStore
import io.github.aedev.flow.youtubesync.runtime.WorkManagerSyncScheduling
import io.github.aedev.flow.youtubesync.runtime.YouTubeSyncNotifier
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** Profile-private, never backed up, outside the Device Sync: the Sync Baseline stays on this device (ADR 0002). */
private val Context.syncBaselineDataStore by safePreferencesDataStore(
    name = "youtube_sync_baseline",
    scope = StoreScope.PROFILE_PRIVATE,
)

/** Profile-private, never backed up: the Linked Account belongs to one profile on this device only. */
private val Context.linkedAccountDataStore by safePreferencesDataStore(
    name = "youtube_sync_account",
    scope = StoreScope.PROFILE_PRIVATE,
)

/** Profile-private, never backed up: where this device's YouTube Sync stands. */
private val Context.syncStatusDataStore by safePreferencesDataStore(
    name = "youtube_sync_status",
    scope = StoreScope.PROFILE_PRIVATE,
)

@Module
@InstallIn(SingletonComponent::class)
abstract class YouTubeSyncModule {
    @Binds
    abstract fun oauthGateway(impl: AppAuthGateway): OAuthGateway

    @Binds
    abstract fun identityApi(impl: GoogleYouTubeIdentityApi): YouTubeIdentityApi

    @Binds
    abstract fun youTubeAccount(impl: YouTubeDataApiAccount): YouTubeAccount

    @Binds
    abstract fun accessTokens(impl: LinkedAccountTokenSource): AccessTokenSource

    @Binds
    abstract fun localSubscriptions(impl: FlowLocalSubscriptions): LocalSubscriptions

    @Binds
    abstract fun localPlaylists(impl: FlowLocalPlaylists): LocalPlaylists

    @Binds
    abstract fun scheduling(impl: WorkManagerSyncScheduling): SyncScheduling

    @Binds
    abstract fun notifications(impl: YouTubeSyncNotifier): SyncNotifications

    @Binds
    abstract fun secretSealer(impl: KeystoreSecretSealer): SecretSealer

    companion object {
        @Provides
        @Singleton
        fun oauthConfig(): OAuthConfig = OAuthConfig.fromBuild()

        @Provides
        @Singleton
        fun thisDevice(
            @ApplicationContext context: Context,
        ): ThisDevice = ThisDevice.from(context)

        @Provides
        fun clock(): Clock = Clock { System.currentTimeMillis() }

        @Provides
        @Singleton
        fun syncStatusStore(
            @ApplicationContext context: Context,
        ): SyncStatusStore = SyncStatusStore(context.syncStatusDataStore)

        @Provides
        @Singleton
        @YouTubeSyncHttp
        fun httpClient(): OkHttpClient = YouTubeSyncHttpClient.create()

        @Provides
        @Singleton
        fun linkedAccountStore(
            @ApplicationContext context: Context,
            sealer: SecretSealer,
        ): LinkedAccountStore = LinkedAccountStore(context.linkedAccountDataStore, sealer)

        @Provides
        @Singleton
        fun syncBaselineStore(
            @ApplicationContext context: Context,
        ): SyncBaselineStore = SyncBaselineStore(context.syncBaselineDataStore)
    }
}
