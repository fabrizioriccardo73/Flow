package io.github.aedev.flow.youtubesync.engine

/** Which side of the YouTube Sync a change lands on. */
enum class SyncSide { FLOW, YOUTUBE }

/** The kinds of things the YouTube Sync carries. Playlists join with ticket 09. */
enum class SyncCollection { SUBSCRIPTIONS, }

/**
 * A YouTube Sync that would remove too many items from one [collection] on one [side]: it must not be
 * applied without the viewer's confirmation (spec: "Protezione dalle rimozioni di massa").
 */
data class MassRemoval(
    val collection: SyncCollection,
    val side: SyncSide,
    /** What would be removed, by display name. */
    val items: List<String>,
    /** How many items the collection holds on that side before the run. */
    val total: Int,
) {
    val count: Int get() = items.size
}

/**
 * Decides when removals are "too many". The usual cause is data lost on one side (a reset, a bug, a
 * wrong account) while the Sync Baseline survives: the comparison then reads as "everything was removed".
 */
object RemovalGuard {
    /** More removals than this always need confirmation. */
    const val MAX_REMOVALS = 20

    /** A share of the collection above this needs confirmation… */
    const val MAX_SHARE = 0.10

    /**
     * …once at least this many items go, so one or two ordinary unsubscribes from a small list never
     * ask: with 5 subscriptions a single one is already 20%.
     */
    const val MIN_REMOVALS_FOR_SHARE = 3

    fun check(
        collection: SyncCollection,
        side: SyncSide,
        removing: List<String>,
        total: Int,
    ): MassRemoval? {
        val count = removing.size
        if (count == 0) return null
        val tooMany = count > MAX_REMOVALS
        val tooLargeShare = count >= MIN_REMOVALS_FOR_SHARE && count > total * MAX_SHARE
        return if (tooMany || tooLargeShare) MassRemoval(collection, side, removing, total) else null
    }
}

/** Thrown by [YouTubeSync.apply] for a plan with [SyncPlan.massRemovals] that was not confirmed. Nothing was written. */
class MassRemovalNotConfirmedException(
    val massRemovals: List<MassRemoval>,
) : Exception("The YouTube Sync would remove too many items and was not confirmed")
