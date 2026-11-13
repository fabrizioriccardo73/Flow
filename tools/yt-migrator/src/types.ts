export interface ChannelSubscription {
  id: string;
  title: string;
  subscribers?: string;
  url?: string;
}

export interface UserPlaylist {
  id: string;
  title: string;
  videoCount?: number;
  videoIds?: string[];
}

export interface AccountProfile {
  id?: string;
  name?: string;
  handle?: string;
  email?: string;
  hasChannel?: boolean;
}

export interface MigrationCheckpoint {
  version: number;
  updatedAt: string;
  sourceAccount: AccountProfile;
  targetAccount: AccountProfile;
  subscriptions: {
    completed: string[];
    skipped: string[];
    failed: { id: string; title: string; error: string }[];
  };
  playlists: {
    completed: Record<string, string>; // sourceId -> targetId
    skipped: string[];
    failed: { id: string; title: string; error: string }[];
  };
}

export interface CLIOptions {
  dryRun?: boolean;
  resume?: boolean;
  onlySubscriptions?: boolean;
  onlyPlaylists?: boolean;
  delay?: number;
  resetAuth?: boolean;
  dataDir?: string;
}
