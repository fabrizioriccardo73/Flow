import type { Innertube } from 'youtubei.js';
import type { ChannelSubscription, UserPlaylist } from './types.js';

export async function fetchSubscriptions(
  yt: Innertube,
  onProgress?: (count: number) => void
): Promise<ChannelSubscription[]> {
  const subscriptionsMap = new Map<string, ChannelSubscription>();

  // Helper to extract channel info from any raw node or tile
  const extractChannelFromNode = (obj: any) => {
    if (!obj || typeof obj !== 'object') return;

    if (
      obj.tileRenderer &&
      (obj.tileRenderer.contentType === 'TILE_CONTENT_TYPE_CHANNEL' ||
        obj.tileRenderer.contentId?.startsWith('UC') ||
        obj.tileRenderer.onSelectCommand?.browseEndpoint?.browseId?.startsWith('UC'))
    ) {
      const tr = obj.tileRenderer;
      const id = tr.contentId || tr.onSelectCommand?.browseEndpoint?.browseId;
      const title =
        tr.metadata?.tileMetadataRenderer?.title?.simpleText ||
        tr.metadata?.tileMetadataRenderer?.title?.runs?.[0]?.text ||
        'Canale';

      const subscribers =
        tr.metadata?.tileMetadataRenderer?.lines?.[1]?.lineRenderer?.items?.[0]?.lineItemRenderer?.text?.simpleText ||
        tr.metadata?.tileMetadataRenderer?.lines?.[0]?.lineRenderer?.items?.[0]?.lineItemRenderer?.text?.simpleText;

      if (id && !subscriptionsMap.has(id)) {
        subscriptionsMap.set(id, {
          id,
          title,
          subscribers,
          url: `https://www.youtube.com/channel/${id}`
        });
        onProgress?.(subscriptionsMap.size);
      }
    }

    // Standard Web client Channel / GridChannel node
    if (obj.type === 'Channel' || obj.type === 'GridChannel') {
      const id = obj.id || obj.author?.id;
      const title = obj.author?.name || obj.title?.toString?.() || 'Canale';
      const subscribers = obj.subscribers?.toString?.() || obj.subscriber_count?.toString?.();
      if (id && !subscriptionsMap.has(id)) {
        subscriptionsMap.set(id, {
          id,
          title,
          subscribers,
          url: `https://www.youtube.com/channel/${id}`
        });
        onProgress?.(subscriptionsMap.size);
      }
    }

    for (const key of Object.keys(obj)) {
      extractChannelFromNode(obj[key]);
    }
  };

  try {
    // 1. Direct browse endpoint call
    const res = await yt.actions.execute('/browse', { browseId: 'FEchannels' });
    if (res?.data) {
      extractChannelFromNode(res.data);
    }
  } catch (err: any) {
    // 2. Fallback to standard getChannelsFeed if direct browse fails
    try {
      let feed = await yt.getChannelsFeed();
      if (feed?.channels && Array.isArray(feed.channels)) {
        for (const ch of feed.channels as any[]) {
          const id = ch.id || ch.author?.id;
          const title = ch.author?.name || ch.title?.toString?.() || 'Canale';
          if (id && !subscriptionsMap.has(id)) {
            subscriptionsMap.set(id, {
              id,
              title,
              url: `https://www.youtube.com/channel/${id}`
            });
            onProgress?.(subscriptionsMap.size);
          }
        }
      }
      while (feed?.has_continuation) {
        feed = await feed.getContinuation();
        if (feed?.channels && Array.isArray(feed.channels)) {
          for (const ch of feed.channels as any[]) {
            const id = ch.id || ch.author?.id;
            const title = ch.author?.name || ch.title?.toString?.() || 'Canale';
            if (id && !subscriptionsMap.has(id)) {
              subscriptionsMap.set(id, {
                id,
                title,
                url: `https://www.youtube.com/channel/${id}`
              });
              onProgress?.(subscriptionsMap.size);
            }
          }
        }
      }
    } catch {
      // Ignore fallback error
    }
  }

  return Array.from(subscriptionsMap.values());
}

export async function fetchPlaylists(
  yt: Innertube,
  onProgress?: (count: number) => void
): Promise<UserPlaylist[]> {
  const playlistsMap = new Map<string, UserPlaylist>();

  const extractPlaylistFromNode = (obj: any) => {
    if (!obj || typeof obj !== 'object') return;

    if (
      obj.tileRenderer &&
      (obj.tileRenderer.contentType === 'TILE_CONTENT_TYPE_PLAYLIST' ||
        obj.tileRenderer.contentId?.startsWith('PL') ||
        obj.tileRenderer.onSelectCommand?.browseEndpoint?.browseId?.startsWith('VLPL') ||
        obj.tileRenderer.onSelectCommand?.browseEndpoint?.browseId?.startsWith('PL'))
    ) {
      const tr = obj.tileRenderer;
      const rawId = tr.contentId || tr.onSelectCommand?.browseEndpoint?.browseId;
      const id = rawId?.replace(/^VL/, '');

      // Exclude system playlists like Watch Later (WL), Liked Videos (LL), Favorites (FL)
      if (id && id !== 'WL' && id !== 'LL' && !id.startsWith('FL')) {
        const title =
          tr.metadata?.tileMetadataRenderer?.title?.simpleText ||
          tr.metadata?.tileMetadataRenderer?.title?.runs?.[0]?.text ||
          'Playlist senza titolo';

        const countText = tr.header?.tileHeaderRenderer?.thumbnailOverlays?.[0]?.thumbnailOverlayTimeStatusRenderer?.text?.runs
          ?.map((r: any) => r.text)
          .join('');

        const videoCount = countText ? parseInt(countText.replace(/\D/g, ''), 10) : undefined;

        if (!playlistsMap.has(id)) {
          playlistsMap.set(id, {
            id,
            title,
            videoCount: isNaN(videoCount as any) ? undefined : videoCount
          });
          onProgress?.(playlistsMap.size);
        }
      }
    }

    // Standard Web Playlist / GridPlaylist node
    if (obj.type === 'Playlist' || obj.type === 'GridPlaylist') {
      const id = obj.id || obj.playlist_id;
      if (id && id !== 'WL' && id !== 'LL' && !id.startsWith('FL') && !playlistsMap.has(id)) {
        const title = obj.title?.toString?.() || 'Playlist senza titolo';
        const videoCountStr = obj.video_count?.toString?.() || obj.video_count_short?.toString?.();
        const videoCount = videoCountStr ? parseInt(videoCountStr.replace(/\D/g, ''), 10) : undefined;

        playlistsMap.set(id, {
          id,
          title,
          videoCount: isNaN(videoCount as any) ? undefined : videoCount
        });
        onProgress?.(playlistsMap.size);
      }
    }

    for (const key of Object.keys(obj)) {
      extractPlaylistFromNode(obj[key]);
    }
  };

  try {
    // 1. Direct browse call
    const res = await yt.actions.execute('/browse', { browseId: 'FEplaylist_aggregation' });
    if (res?.data) {
      extractPlaylistFromNode(res.data);
    }
  } catch {
    // 2. Fallback to standard getPlaylists
    try {
      let feed = await yt.getPlaylists();
      if (feed?.playlists && Array.isArray(feed.playlists)) {
        for (const pl of feed.playlists as any[]) {
          const id = pl.id;
          if (id && id !== 'WL' && id !== 'LL' && !playlistsMap.has(id)) {
            playlistsMap.set(id, {
              id,
              title: pl.title?.toString?.() || 'Playlist',
              videoCount: undefined
            });
            onProgress?.(playlistsMap.size);
          }
        }
      }
    } catch {
      // Ignore
    }
  }

  return Array.from(playlistsMap.values());
}

export async function fetchPlaylistVideoIds(
  yt: Innertube,
  playlistId: string
): Promise<string[]> {
  const videoIds = new Set<string>();

  const extractVideosFromNode = (obj: any) => {
    if (!obj || typeof obj !== 'object') return;

    if (obj.tileRenderer && obj.tileRenderer.contentId && obj.tileRenderer.contentType === 'TILE_CONTENT_TYPE_VIDEO') {
      videoIds.add(obj.tileRenderer.contentId);
    } else if (obj.videoId && typeof obj.videoId === 'string' && obj.videoId.length === 11) {
      videoIds.add(obj.videoId);
    }

    for (const key of Object.keys(obj)) {
      extractVideosFromNode(obj[key]);
    }
  };

  try {
    const browseId = playlistId.startsWith('VL') ? playlistId : `VL${playlistId}`;
    const res = await yt.actions.execute('/browse', { browseId });
    if (res?.data) {
      extractVideosFromNode(res.data);
    }
  } catch {
    // Fallback to getPlaylist
    try {
      let playlist = await yt.getPlaylist(playlistId);
      if (playlist?.items && Array.isArray(playlist.items)) {
        for (const item of playlist.items) {
          const id = (item as any)?.id || (item as any)?.video_id;
          if (id && typeof id === 'string') videoIds.add(id);
        }
      }
      while (playlist?.has_continuation) {
        playlist = await playlist.getContinuation();
        if (playlist?.items && Array.isArray(playlist.items)) {
          for (const item of playlist.items) {
            const id = (item as any)?.id || (item as any)?.video_id;
            if (id && typeof id === 'string') videoIds.add(id);
          }
        }
      }
    } catch {
      // Ignore
    }
  }

  return Array.from(videoIds);
}
