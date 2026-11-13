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
import io.github.aedev.flow.youtubesync.account.OAuthGateway
import io.github.aedev.flow.youtubesync.account.SecretSealer
import io.github.aedev.flow.youtubesync.account.YouTubeIdentityApi
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttp
import io.github.aedev.flow.youtubesync.net.YouTubeSyncHttpClient
import okhttp3.OkHttpClient
import javax.inject.Singleton

/** Profile-private, never backed up: the Linked Account belongs to one profile on this device only. */
private val Context.linkedAccountDataStore by safePreferencesDataStore(
    name = "youtube_sync_account",
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
    abstract fun secretSealer(impl: KeystoreSecretSealer): SecretSealer

    companion object {
        @Provides
        @Singleton
        fun oauthConfig(): OAuthConfig = OAuthConfig.fromBuild()

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
    }
}
