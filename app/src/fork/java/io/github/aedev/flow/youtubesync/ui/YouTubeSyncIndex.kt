package io.github.aedev.flow.youtubesync.ui

import io.github.aedev.flow.R
import io.github.aedev.flow.ui.components.settings.SettingEntry
import io.github.aedev.flow.ui.components.settings.SettingsDestination

/** Settings entries of the YouTube Sync page. */
object YouTubeSyncIndex {
    private val page = SettingsDestination.YOUTUBE_SYNC

    val link =
        SettingEntry(
            key = "youtube_sync.link",
            title = R.string.youtube_sync_link,
            summary = R.string.youtube_sync_link_summary,
            section = R.string.youtube_sync_account_section,
            destination = page,
        )

    val syncNow =
        SettingEntry(
            key = "youtube_sync.sync_now",
            title = R.string.youtube_sync_now,
            summary = R.string.youtube_sync_now_summary,
            section = R.string.youtube_sync_sync_section,
            destination = page,
        )

    val account =
        SettingEntry(
            key = "youtube_sync.account",
            title = R.string.youtube_sync_account,
            section = R.string.youtube_sync_account_section,
            destination = page,
        )
}
