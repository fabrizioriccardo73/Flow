package io.github.aedev.flow.youtubesync.engine

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files

/** Flow, a Linked Account and a Sync Baseline on a temp file: the primary seam, for playlist tests. */
open class SyncFixture {
    private val dir: File = Files.createTempDirectory("youtube-sync").toFile()
    private val job = Job()
    val baselines =
        SyncBaselineStore(
            PreferenceDataStoreFactory.create(scope = CoroutineScope(Dispatchers.IO + job)) { File(dir, "baseline.preferences_pb") },
        )
    val subscriptions = FakeLocalSubscriptions()
    val playlists = FakeLocalPlaylists()
    val youTube = FakeYouTubeAccount()
    val sync = YouTubeSync(subscriptions, youTube, baselines, playlists)

    suspend fun syncNow(confirm: Boolean = false) = sync.apply(sync.plan(), massRemovalConfirmed = confirm)

    fun close() {
        runBlocking { job.cancelAndJoin() }
        dir.deleteRecursively()
    }
}
