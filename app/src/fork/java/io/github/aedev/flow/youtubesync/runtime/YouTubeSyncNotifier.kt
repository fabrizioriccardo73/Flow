package io.github.aedev.flow.youtubesync.runtime

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.aedev.flow.MainActivity
import io.github.aedev.flow.R
import io.github.aedev.flow.data.profile.ActiveProfile
import io.github.aedev.flow.notification.NotificationHelper
import javax.inject.Inject
import javax.inject.Singleton

/** [SyncNotifications] in the profile's general channel; a tap opens the YouTube Sync page. */
@Singleton
class YouTubeSyncNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SyncNotifications {
        override fun needsRelink() =
            show(
                RELINK_ID,
                context.getString(R.string.youtube_sync_relink_notification_title),
                context.getString(R.string.youtube_sync_relink_notification_text),
            )

        override fun needsConfirmation(removals: Int) =
            show(
                CONFIRM_ID,
                context.getString(R.string.youtube_sync_confirm_notification_title),
                context.getString(R.string.youtube_sync_confirm_notification_text, removals),
            )

        override fun clear() {
            val manager = NotificationManagerCompat.from(context)
            manager.cancel(RELINK_ID)
            manager.cancel(CONFIRM_ID)
        }

        @android.annotation.SuppressLint("MissingPermission") // checked by hasNotificationPermission
        private fun show(
            id: Int,
            title: String,
            text: String,
        ) {
            if (!NotificationHelper.hasNotificationPermission(context)) return
            val intent =
                Intent(context, MainActivity::class.java)
                    .putExtra(NotificationHelper.EXTRA_PROFILE_ID, ActiveProfile.get(context).id)
                    .putExtra(EXTRA_OPEN_YOUTUBE_SYNC, true)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pending = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification =
                NotificationCompat
                    .Builder(context, NotificationHelper.CHANNEL_GENERAL)
                    .setSmallIcon(R.drawable.ic_notification_logo)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify(id, notification)
        }

        companion object {
            /** On the launch intent: open the YouTube Sync page (handled in IncomingIntents). */
            const val EXTRA_OPEN_YOUTUBE_SYNC = "io.github.fabrizioriccardo73.flow.extra.OPEN_YOUTUBE_SYNC"
            private const val RELINK_ID = 4_901
            private const val CONFIRM_ID = 4_902
        }
    }
