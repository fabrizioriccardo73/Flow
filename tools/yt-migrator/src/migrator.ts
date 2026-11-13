import type { Innertube } from 'youtubei.js';
import type {
  ChannelSubscription,
  UserPlaylist,
  MigrationCheckpoint,
  CLIOptions
} from './types.js';
import type { StorageManager } from './storage.js';
import { fetchPlaylistVideoIds } from './scanner.js';

export const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

export function getJitterDelay(baseDelayMs: number): number {
  // Adds 0 to 500ms random jitter to avoid fixed cadence
  return baseDelayMs + Math.floor(Math.random() * 500);
}

export interface MigrationEvents {
  onSubscriptionProgress?: (current: number, total: number, channel: ChannelSubscription, status: 'migrated' | 'skipped' | 'failed') => void;
  onPlaylistProgress?: (current: number, total: number, playlist: UserPlaylist, status: 'migrated' | 'skipped' | 'failed', videoCount?: number) => void;
  onLog?: (message: string) => void;
}

export async function migrateSubscriptions(
  ytTarget: Innertube,
  channels: ChannelSubscription[],
  targetExistingChannelIds: Set<string>,
  checkpoint: MigrationCheckpoint,
  storage: StorageManager,
  options: CLIOptions,
  events?: MigrationEvents
): Promise<{ migrated: number; skipped: number; failed: number }> {
  let migrated = 0;
  let skipped = 0;
  let failed = 0;

  const baseDelay = options.delay ?? 1000;
  const isDryRun = !!options.dryRun;

  for (let i = 0; i < channels.length; i++) {
    const channel = channels[i];

    // Check if already completed in checkpoint
    if (checkpoint.subscriptions.completed.includes(channel.id)) {
      skipped++;
      events?.onSubscriptionProgress?.(i + 1, channels.length, channel, 'skipped');
      continue;
    }

    // Check if target account already follows this channel
    if (targetExistingChannelIds.has(channel.id)) {
      skipped++;
      if (!checkpoint.subscriptions.skipped.includes(channel.id)) {
        checkpoint.subscriptions.skipped.push(channel.id);
        storage.saveCheckpoint(checkpoint);
      }
      events?.onSubscriptionProgress?.(i + 1, channels.length, channel, 'skipped');
      continue;
    }

    if (isDryRun) {
      migrated++;
      events?.onSubscriptionProgress?.(i + 1, channels.length, channel, 'migrated');
      continue;
    }

    // Apply jitter delay before write operation
    await sleep(getJitterDelay(baseDelay));

    try {
      await ytTarget.interact.subscribe(channel.id);
      migrated++;
      checkpoint.subscriptions.completed.push(channel.id);
      storage.saveCheckpoint(checkpoint);
      events?.onSubscriptionProgress?.(i + 1, channels.length, channel, 'migrated');
    } catch (err: any) {
      failed++;
      const errorMessage = err?.message || String(err);
      checkpoint.subscriptions.failed.push({
        id: channel.id,
        title: channel.title,
        error: errorMessage
      });
      storage.saveCheckpoint(checkpoint);
      events?.onSubscriptionProgress?.(i + 1, channels.length, channel, 'failed');
    }
  }

  return { migrated, skipped, failed };
}

export type PlaylistConflictStrategy = 'merge' | 'duplicate' | 'skip';

export async function migratePlaylists(
  ytSource: Innertube,
  ytTarget: Innertube,
  ytTargetWriter: Innertube,
  playlists: UserPlaylist[],
  targetExistingPlaylists: UserPlaylist[],
  conflictStrategy: PlaylistConflictStrategy,
  checkpoint: MigrationCheckpoint,
  storage: StorageManager,
  options: CLIOptions,
  events?: MigrationEvents
): Promise<{ migrated: number; skipped: number; failed: number }> {
  let migrated = 0;
  let skipped = 0;
  let failed = 0;

  const baseDelay = options.delay ?? 1000;
  const isDryRun = !!options.dryRun;

  const targetPlaylistsByName = new Map<string, UserPlaylist>();
  for (const pl of targetExistingPlaylists) {
    targetPlaylistsByName.set(pl.title.trim().toLowerCase(), pl);
  }

  // Clear previous failed items before retrying
  checkpoint.playlists.failed = [];

  for (let i = 0; i < playlists.length; i++) {
    const playlist = playlists[i];

    // Already migrated according to checkpoint?
    if (checkpoint.playlists.completed[playlist.id]) {
      skipped++;
      events?.onPlaylistProgress?.(i + 1, playlists.length, playlist, 'skipped');
      continue;
    }

    const existingMatch = targetPlaylistsByName.get(playlist.title.trim().toLowerCase());

    if (existingMatch && conflictStrategy === 'skip') {
      skipped++;
      if (!checkpoint.playlists.skipped.includes(playlist.id)) {
        checkpoint.playlists.skipped.push(playlist.id);
        storage.saveCheckpoint(checkpoint);
      }
      events?.onPlaylistProgress?.(i + 1, playlists.length, playlist, 'skipped');
      continue;
    }

    if (isDryRun) {
      migrated++;
      events?.onPlaylistProgress?.(i + 1, playlists.length, playlist, 'migrated', playlist.videoCount ?? 0);
      continue;
    }

    // Fetch video IDs from source
    await sleep(getJitterDelay(baseDelay));
    const videoIds = await fetchPlaylistVideoIds(ytSource, playlist.id);

    try {
      let targetPlaylistId: string | undefined;

      if (existingMatch && conflictStrategy === 'merge') {
        targetPlaylistId = existingMatch.id;
        // Fetch existing videos on target to avoid duplicates
        const existingVideos = await fetchPlaylistVideoIds(ytTarget, targetPlaylistId);
        const existingSet = new Set(existingVideos);
        const missingVideoIds = videoIds.filter((id) => !existingSet.has(id));

        if (missingVideoIds.length > 0) {
          // Batch in chunks of 50 videos
          for (let c = 0; c < missingVideoIds.length; c += 50) {
            const chunk = missingVideoIds.slice(c, c + 50);
            await sleep(getJitterDelay(baseDelay));
            await ytTargetWriter.playlist.addVideos(targetPlaylistId, chunk);
          }
        }
      } else {
        // Create new playlist with initial videos (first 50)
        const initialChunk = videoIds.slice(0, 50);
        const remainingChunks = videoIds.slice(50);

        await sleep(getJitterDelay(baseDelay));
        const res = await ytTargetWriter.playlist.create(playlist.title, initialChunk);

        targetPlaylistId = res?.playlist_id;

        if (targetPlaylistId && remainingChunks.length > 0) {
          for (let c = 0; c < remainingChunks.length; c += 50) {
            const chunk = remainingChunks.slice(c, c + 50);
            await sleep(getJitterDelay(baseDelay));
            await ytTargetWriter.playlist.addVideos(targetPlaylistId, chunk);
          }
        }
      }

      if (targetPlaylistId) {
        migrated++;
        checkpoint.playlists.completed[playlist.id] = targetPlaylistId;
        storage.saveCheckpoint(checkpoint);
        events?.onPlaylistProgress?.(i + 1, playlists.length, playlist, 'migrated', videoIds.length);
      } else {
        throw new Error('Impossibile ottenere l\'ID della playlist creata.');
      }
    } catch (err: any) {
      failed++;
      const errorMessage = err?.message || String(err);
      checkpoint.playlists.failed.push({
        id: playlist.id,
        title: playlist.title,
        error: errorMessage
      });
      storage.saveCheckpoint(checkpoint);
      events?.onPlaylistProgress?.(i + 1, playlists.length, playlist, 'failed');
    }
  }

  return { migrated, skipped, failed };
}
