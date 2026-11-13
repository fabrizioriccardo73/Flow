package io.github.aedev.flow.youtubesync.account

import android.content.Intent
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

/** The Linked Account seam: link, use and unlink through fakes of Google and AppAuth. */
class LinkedAccountManagerTest {
    private lateinit var dir: File
    private lateinit var job: Job
    private lateinit var storeFile: File
    private lateinit var store: LinkedAccountStore
    private val oauth = FakeOAuth()
    private val identity = FakeIdentity()
    private lateinit var manager: LinkedAccountManager

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("linked-account").toFile()
        storeFile = File(dir, "youtube_sync_account.preferences_pb")
        job = Job()
        val dataStore = PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { storeFile }
        store = LinkedAccountStore(dataStore, ReversingSealer)
        manager = LinkedAccountManager(store, oauth, identity, OAuthConfig("client", "app:/oauth2redirect"))
    }

    @After
    fun tearDown() {
        runBlocking { job.cancelAndJoin() }
        dir.deleteRecursively()
    }

    @Test
    fun `linking saves the account's channel and its tokens`() =
        runBlocking<Unit> {
            val account = manager.completeLink(null)

            assertThat(account).isEqualTo(LinkedAccount("UC123", "My channel"))
            assertThat(manager.account.first()).isEqualTo(account)
            assertThat(identity.seenAccessTokens).containsExactly("access-1")
        }

    @Test
    fun `the refresh token is never written in clear`() =
        runBlocking<Unit> {
            manager.completeLink(null)

            val raw = storeFile.readText(Charsets.ISO_8859_1)
            assertThat(raw).doesNotContain("refresh-secret")
            assertThat(store.authState()).contains("refresh-secret")
        }

    @Test
    fun `a cancelled authorization links nothing`() =
        runBlocking<Unit> {
            oauth.failAuthorization = LinkedAccountException.Cancelled()

            assertThrows(LinkedAccountException.Cancelled::class.java) { runBlocking { manager.completeLink(null) } }
            assertThat(manager.account.first()).isNull()
        }

    @Test
    fun `an access token is refreshed and the new auth state is saved`() =
        runBlocking<Unit> {
            manager.completeLink(null)
            oauth.nextRefresh = OAuthGateway.FreshToken("access-2", "state(refresh-secret,v2)")

            assertThat(manager.accessToken()).isEqualTo("access-2")
            assertThat(store.authState()).isEqualTo("state(refresh-secret,v2)")
        }

    @Test
    fun `asking for a token with no linked account fails as not linked`() {
        assertThrows(LinkedAccountException.NotLinked::class.java) { runBlocking { manager.accessToken() } }
    }

    @Test
    fun `unlinking revokes the grant and forgets the account`() =
        runBlocking<Unit> {
            manager.completeLink(null)

            manager.unlink()

            assertThat(identity.revoked).containsExactly("refresh-secret")
            assertThat(manager.account.first()).isNull()
            assertThat(store.authState()).isNull()
        }

    @Test
    fun `unlinking forgets the account even when revoking fails`() =
        runBlocking<Unit> {
            manager.completeLink(null)
            identity.failRevoke = true

            manager.unlink()

            assertThat(manager.account.first()).isNull()
            assertThat(store.authState()).isNull()
        }

    @Test
    fun `linking again replaces the previous account`() =
        runBlocking<Unit> {
            manager.completeLink(null)
            identity.channel = LinkedAccount("UC999", "Other channel")

            manager.completeLink(null)

            assertThat(manager.account.first()).isEqualTo(LinkedAccount("UC999", "Other channel"))
        }

    /** Reverses the text: enough to prove the stored bytes are not the plain secret. */
    private object ReversingSealer : SecretSealer {
        override fun seal(plain: String) = "sealed:" + plain.reversed()

        override fun open(sealed: String?) = sealed?.removePrefix("sealed:")?.reversed().orEmpty()
    }

    private class FakeOAuth : OAuthGateway {
        var failAuthorization: LinkedAccountException? = null
        var nextRefresh: OAuthGateway.FreshToken? = null

        override fun authorizationIntent(): Intent = throw UnsupportedOperationException()

        override suspend fun completeAuthorization(result: Intent?): String {
            failAuthorization?.let { throw it }
            return "state(refresh-secret,v1)"
        }

        override suspend fun freshAccessToken(authState: String): OAuthGateway.FreshToken =
            nextRefresh ?: OAuthGateway.FreshToken("access-1", authState)

        override fun revocableToken(authState: String): String? = if ("refresh-secret" in authState) "refresh-secret" else null
    }

    private class FakeIdentity : YouTubeIdentityApi {
        var channel = LinkedAccount("UC123", "My channel")
        var failRevoke = false
        val seenAccessTokens = mutableListOf<String>()
        val revoked = mutableListOf<String>()

        override suspend fun ownChannel(accessToken: String): LinkedAccount {
            seenAccessTokens += accessToken
            return channel
        }

        override suspend fun revoke(token: String) {
            if (failRevoke) throw LinkedAccountException.Transient()
            revoked += token
        }
    }
}
